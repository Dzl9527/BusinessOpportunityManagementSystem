#!/usr/bin/env node

const DEFAULT_BASE_URL = "https://open.feishu.cn";
const DEFAULT_TIME_ZONE = "Asia/Shanghai";
const DEFAULT_DAYS = 7;
const PAGE_SIZE = 100;

main().catch((error) => {
  console.error(`查询失败：${error.message}`);
  if (error.details) {
    console.error(error.details);
  }
  process.exitCode = 1;
});

async function main() {
  const options = parseArgs(process.argv.slice(2));

  if (options.help) {
    printHelp();
    return;
  }

  const baseUrl = trimTrailingSlash(
    options["base-url"] || process.env.FEISHU_BASE_URL || DEFAULT_BASE_URL,
  );
  const timeZone = options.timezone || process.env.FEISHU_TIMEZONE || DEFAULT_TIME_ZONE;
  const command = options._[0] || (options["list-calendars"] ? "calendars" : "events");
  const auth = await resolveAccessToken(baseUrl);

  if (command === "calendars") {
    const calendars = await listCalendars(baseUrl, auth.token);
    printCalendars(calendars);
    return;
  }

  if (command !== "events") {
    throw new Error(`未知命令：${command}`);
  }

  const { from, to } = resolveRange(options);
  const calendarTargets = await resolveCalendars(baseUrl, auth.token, options, auth.kind);
  const events = [];

  for (const calendar of calendarTargets) {
    const calendarEvents = await listEvents(baseUrl, auth.token, calendar.id, from, to);
    events.push(
      ...calendarEvents.map((event) => ({
        ...event,
        __calendarId: calendar.id,
        __calendarName: calendar.summary || calendar.id,
      })),
    );
  }

  events.sort(compareEvents);

  if (options.json) {
    console.log(JSON.stringify({ range: { from, to }, calendars: calendarTargets, events }, null, 2));
    return;
  }

  printEvents(events, { from, to, timeZone, calendars: calendarTargets });
}

function parseArgs(argv) {
  const options = { _: [] };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];

    if (!arg.startsWith("--")) {
      options._.push(arg);
      continue;
    }

    const raw = arg.slice(2);
    if (raw.includes("=")) {
      const [key, ...valueParts] = raw.split("=");
      options[key] = valueParts.join("=");
      continue;
    }

    const next = argv[index + 1];
    if (!next || next.startsWith("--")) {
      options[raw] = true;
      continue;
    }

    options[raw] = next;
    index += 1;
  }

  return options;
}

async function resolveAccessToken(baseUrl) {
  const userToken = envValue("FEISHU_USER_ACCESS_TOKEN", "LARK_USER_ACCESS_TOKEN");
  if (userToken) {
    return { token: userToken, kind: "user" };
  }

  const tenantToken = envValue("FEISHU_TENANT_ACCESS_TOKEN", "LARK_TENANT_ACCESS_TOKEN");
  if (tenantToken) {
    return { token: tenantToken, kind: "tenant" };
  }

  const appId = envValue("FEISHU_APP_ID", "LARK_APP_ID");
  const appSecret = envValue("FEISHU_APP_SECRET", "LARK_APP_SECRET");

  if (!appId || !appSecret) {
    throw new Error(
      [
        "缺少飞书访问凭证。",
        "查询个人日历请设置 FEISHU_USER_ACCESS_TOKEN。",
        "如果只查应用身份可访问的日历，也可以设置 FEISHU_APP_ID 和 FEISHU_APP_SECRET。",
      ].join("\n"),
    );
  }

  const response = await requestJson(baseUrl, "/open-apis/auth/v3/tenant_access_token/internal", {
    method: "POST",
    body: { app_id: appId, app_secret: appSecret },
  });

  const token = response.tenant_access_token || response.data?.tenant_access_token;
  if (!token) {
    throw new Error("飞书未返回 tenant_access_token，请检查应用 ID、密钥和应用状态。");
  }

  return { token, kind: "tenant" };
}

async function resolveCalendars(baseUrl, token, options, authKind) {
  const explicitCalendarId = options.calendar || options["calendar-id"] || process.env.FEISHU_CALENDAR_ID;

  if (explicitCalendarId) {
    const calendarId = explicitCalendarId === "primary"
      ? await getPrimaryCalendarId(baseUrl, token)
      : explicitCalendarId;
    return [{ id: calendarId, summary: explicitCalendarId === "primary" ? "主日历" : explicitCalendarId }];
  }

  if (options["all-calendars"]) {
    const calendars = await listCalendars(baseUrl, token);
    if (calendars.length === 0) {
      throw new Error("没有找到当前 token 可访问的日历。");
    }
    return calendars.map((calendar) => ({
      id: calendar.calendar_id,
      summary: calendar.summary || calendar.summary_alias || calendar.calendar_id,
    }));
  }

  if (authKind === "user") {
    try {
      return [{ id: await getPrimaryCalendarId(baseUrl, token), summary: "主日历" }];
    } catch (error) {
      console.warn(`未能读取主日历，改为读取日历列表：${error.message}`);
    }
  }

  const calendars = await listCalendars(baseUrl, token);
  const primary = calendars.find((calendar) => calendar.type === "primary" || calendar.is_primary);
  const chosen = primary || calendars[0];

  if (!chosen) {
    throw new Error(
      authKind === "tenant"
        ? "应用身份没有可访问的日历。查询个人日历通常需要 FEISHU_USER_ACCESS_TOKEN。"
        : "没有找到当前 token 可访问的日历。",
    );
  }

  return [{
    id: chosen.calendar_id,
    summary: chosen.summary || chosen.summary_alias || chosen.calendar_id,
  }];
}

async function getPrimaryCalendarId(baseUrl, token) {
  const response = await requestJson(baseUrl, "/open-apis/calendar/v4/calendars/primary", {
    method: "POST",
    token,
  });

  const primaryCalendar = firstValue(
    response.data?.calendar,
    response.data?.calendars?.[0]?.calendar,
    response.data?.calendars?.[0],
    response.data,
    response.calendar,
  );
  const calendarId = primaryCalendar?.calendar_id || primaryCalendar?.id;

  if (!calendarId) {
    throw new Error("飞书未返回主日历 ID。");
  }

  return calendarId;
}

async function listCalendars(baseUrl, token) {
  const calendars = [];
  let pageToken = "";

  do {
    const params = new URLSearchParams({ page_size: String(PAGE_SIZE) });
    if (pageToken) {
      params.set("page_token", pageToken);
    }

    const response = await requestJson(baseUrl, `/open-apis/calendar/v4/calendars?${params}`, {
      token,
    });
    const data = response.data || {};
    const items = data.items || data.calendar_list || data.calendars || [];
    calendars.push(...items.map((item) => item.calendar || item));
    pageToken = data.page_token || data.next_page_token || "";

    if (!data.has_more && !data.page_token && !data.next_page_token) {
      break;
    }
  } while (pageToken);

  return calendars.filter((calendar) => calendar && !calendar.is_deleted);
}

async function listEvents(baseUrl, token, calendarId, from, to) {
  const events = [];
  let pageToken = "";

  do {
    const params = new URLSearchParams({
      page_size: String(PAGE_SIZE),
      start_time: toUnixSeconds(from),
      end_time: toUnixSeconds(to),
    });
    if (pageToken) {
      params.set("page_token", pageToken);
    }

    const response = await requestJson(
      baseUrl,
      `/open-apis/calendar/v4/calendars/${encodeURIComponent(calendarId)}/events?${params}`,
      { token },
    );
    const data = response.data || {};
    const items = data.items || data.event_list || data.events || [];
    events.push(...items);
    pageToken = data.page_token || data.next_page_token || "";

    if (!data.has_more && !data.page_token && !data.next_page_token) {
      break;
    }
  } while (pageToken);

  return events;
}

async function requestJson(baseUrl, path, options = {}) {
  const headers = {
    "Content-Type": "application/json; charset=utf-8",
    ...(options.headers || {}),
  };

  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }

  const response = await fetch(`${baseUrl}${path}`, {
    method: options.method || "GET",
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
  const text = await response.text();
  const body = text ? JSON.parse(text) : {};

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${body.msg || body.message || text || "请求失败"}`);
  }

  if (typeof body.code === "number" && body.code !== 0) {
    const error = new Error(`${body.msg || body.message || "飞书接口返回错误"}（code=${body.code}）`);
    error.details = JSON.stringify(body, null, 2);
    throw error;
  }

  return body;
}

function resolveRange(options) {
  const now = new Date();
  const from = options.from ? parseDateTime(options.from) : startOfDay(now);
  const to = options.to
    ? parseDateTime(options.to)
    : addDays(from, numberOption(options.days, DEFAULT_DAYS));

  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime())) {
    throw new Error("日期格式不正确，请使用 YYYY-MM-DD 或 ISO 时间。");
  }

  if (to <= from) {
    throw new Error("--to 必须晚于 --from。");
  }

  return { from, to };
}

function parseDateTime(value) {
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [year, month, day] = value.split("-").map(Number);
    return new Date(year, month - 1, day, 0, 0, 0, 0);
  }

  return new Date(value);
}

function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0);
}

function addDays(date, days) {
  const copy = new Date(date);
  copy.setDate(copy.getDate() + days);
  return copy;
}

function toUnixSeconds(date) {
  return String(Math.floor(date.getTime() / 1000));
}

function compareEvents(left, right) {
  return eventSortValue(left) - eventSortValue(right);
}

function eventSortValue(event) {
  const raw = firstValue(
    event.start_time?.timestamp,
    event.start_time?.date,
    event.start?.timestamp,
    event.start?.date,
    event.start,
  );

  if (!raw) {
    return Number.MAX_SAFE_INTEGER;
  }

  if (/^\d+$/.test(String(raw))) {
    return Number(raw) * 1000;
  }

  const date = parseDateTime(String(raw));
  return Number.isNaN(date.getTime()) ? Number.MAX_SAFE_INTEGER : date.getTime();
}

function printCalendars(calendars) {
  if (calendars.length === 0) {
    console.log("当前 token 没有可访问的日历。");
    return;
  }

  console.log(`找到 ${calendars.length} 个日历：`);
  calendars.forEach((calendar, index) => {
    const name = calendar.summary || calendar.summary_alias || "(未命名日历)";
    console.log(`${index + 1}. ${name}`);
    console.log(`   id: ${calendar.calendar_id}`);
    if (calendar.description) {
      console.log(`   说明: ${singleLine(calendar.description)}`);
    }
  });
}

function printEvents(events, context) {
  const { from, to, timeZone, calendars } = context;
  const rangeText = `${formatDateTime(from, timeZone)} 到 ${formatDateTime(to, timeZone)}`;
  const calendarText = calendars.map((calendar) => calendar.summary || calendar.id).join("、");

  console.log(`查询范围：${rangeText}`);
  console.log(`日历：${calendarText}`);

  if (events.length === 0) {
    console.log("没有查到日程。");
    return;
  }

  console.log(`共 ${events.length} 个日程：`);
  events.forEach((event, index) => {
    const title = event.summary || event.title || "(无标题)";
    const timeText = formatEventTime(event, timeZone);
    console.log(`${index + 1}. ${timeText}  ${singleLine(title)}`);

    const location = locationText(event.location);
    if (location) {
      console.log(`   地点：${location}`);
    }

    const organizer = organizerText(event);
    if (organizer) {
      console.log(`   组织者：${organizer}`);
    }

    if (event.__calendarName) {
      console.log(`   日历：${event.__calendarName}`);
    }

    const link = event.app_link || event.html_link || event.url;
    if (link) {
      console.log(`   链接：${link}`);
    }
  });
}

function formatEventTime(event, timeZone) {
  const start = event.start_time || event.start || {};
  const end = event.end_time || event.end || {};

  if (start.date || end.date) {
    return `${start.date || "全天"}${end.date ? ` 至 ${end.date}` : ""}`;
  }

  const startDate = dateFromFeishuTime(start);
  const endDate = dateFromFeishuTime(end);

  if (!startDate && !endDate) {
    return "(时间未知)";
  }

  if (!endDate) {
    return formatDateTime(startDate, timeZone);
  }

  const sameDay = formatDate(startDate, timeZone) === formatDate(endDate, timeZone);
  if (sameDay) {
    return `${formatDateTime(startDate, timeZone)}-${formatTime(endDate, timeZone)}`;
  }

  return `${formatDateTime(startDate, timeZone)} 至 ${formatDateTime(endDate, timeZone)}`;
}

function dateFromFeishuTime(value) {
  const raw = firstValue(value.timestamp, value.date_time, value.datetime, value);
  if (!raw) {
    return undefined;
  }

  if (/^\d+$/.test(String(raw))) {
    return new Date(Number(raw) * 1000);
  }

  const date = new Date(raw);
  return Number.isNaN(date.getTime()) ? undefined : date;
}

function formatDateTime(date, timeZone) {
  return new Intl.DateTimeFormat("zh-CN", {
    timeZone,
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
}

function formatDate(date, timeZone) {
  return new Intl.DateTimeFormat("zh-CN", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

function formatTime(date, timeZone) {
  return new Intl.DateTimeFormat("zh-CN", {
    timeZone,
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
}

function locationText(location) {
  if (!location) {
    return "";
  }

  if (typeof location === "string") {
    return singleLine(location);
  }

  return singleLine(firstValue(location.name, location.address, location.address_detail, location.display_name, ""));
}

function organizerText(event) {
  const organizer = event.event_organizer || event.organizer || {};
  return singleLine(firstValue(
    organizer.display_name,
    organizer.name,
    organizer.email,
    organizer.user_id,
    organizer.open_id,
    "",
  ));
}

function envValue(...names) {
  for (const name of names) {
    const value = process.env[name]?.trim();
    if (value) {
      return value;
    }
  }

  return "";
}

function numberOption(value, fallback) {
  if (value === undefined || value === true) {
    return fallback;
  }

  const number = Number(value);
  if (!Number.isInteger(number) || number <= 0) {
    throw new Error(`天数必须是正整数：${value}`);
  }

  return number;
}

function singleLine(value) {
  return String(value).replace(/\s+/g, " ").trim();
}

function trimTrailingSlash(value) {
  return value.replace(/\/+$/, "");
}

function firstValue(...values) {
  return values.find((value) => value !== undefined && value !== null && value !== "");
}

function printHelp() {
  console.log(`
飞书日历查询

用法：
  node tools/feishu-calendar.mjs events [--from YYYY-MM-DD] [--to YYYY-MM-DD] [--days 7]
  node tools/feishu-calendar.mjs calendars

常用环境变量：
  FEISHU_USER_ACCESS_TOKEN       查询个人日历时优先使用
  FEISHU_CALENDAR_ID             指定日历 ID；也可在命令里传 --calendar-id
  FEISHU_APP_ID                  自建应用 App ID，仅用于应用身份查询
  FEISHU_APP_SECRET              自建应用 App Secret，仅用于应用身份查询
  FEISHU_BASE_URL                默认 https://open.feishu.cn；Lark 国际版可设为 https://open.larksuite.com

示例：
  node tools/feishu-calendar.mjs calendars
  node tools/feishu-calendar.mjs events --days 1
  node tools/feishu-calendar.mjs events --from 2026-05-29 --to 2026-05-30 --calendar-id primary
  node tools/feishu-calendar.mjs events --all-calendars --days 7 --json
`.trim());
}
