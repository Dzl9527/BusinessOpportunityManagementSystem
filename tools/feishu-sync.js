const fs = require('fs');
const path = require('path');

// ==================== CONFIGURATION ====================
const APP_ID = process.env.FEISHU_APP_ID || 'cli_xxxxxxxxxxxx';
const APP_SECRET = process.env.FEISHU_APP_SECRET || 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx';
// Optional: specify parent folder token if you want documents to be imported in a specific folder
const FOLDER_TOKEN = process.env.FEISHU_FOLDER_TOKEN || '';
// ========================================================

const FILES_TO_SYNC = [
  {
    localPath: 'docs/requirements/2026-05-24-用户端商机提报数据结构设计.md',
    title: '用户端商机提报数据结构设计'
  },
  {
    localPath: 'docs/requirements/2026-05-24-设备事业群商机字段改造需求.md',
    title: '设备事业群商机字段改造需求'
  },
  {
    localPath: 'docs/requirements/2026-05-25-OA商机报备与项目授权申请同步需求.md',
    title: 'OA商机报备与项目授权申请同步需求'
  },
  {
    localPath: 'docs/requirements/2026-05-26-手机端信息架构与核心页面需求.md',
    title: '手机端信息架构与核心页面需求'
  },
  {
    localPath: 'docs/design/2026-05-26-OA商机报备与项目授权申请流程图.md',
    title: 'OA商机报备与项目授权申请流程图'
  }
];

async function getTenantAccessToken() {
  if (APP_ID.includes('cli_xx')) {
    throw new Error('Please set your real FEISHU_APP_ID and FEISHU_APP_SECRET!');
  }
  
  const res = await fetch('https://open-apis.feishu.cn/open-apis/auth/v3/tenant_access_token/internal', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      app_id: APP_ID,
      app_secret: APP_SECRET
    })
  });
  
  const data = await res.json();
  if (data.code !== 0) {
    throw new Error(`Failed to get tenant_access_token: ${data.msg} (code: ${data.code})`);
  }
  return data.tenant_access_token;
}

async function uploadFile(token, filePath, fileName) {
  const fileBuffer = fs.readFileSync(filePath);
  const size = fileBuffer.length;
  
  // Use Node.js built-in FormData (Node 18+)
  const formData = new FormData();
  formData.append('file_name', fileName);
  formData.append('parent_type', 'ccm_import_open');
  formData.append('size', String(size));
  // Convert Buffer to Blob for native fetch FormData
  const blob = new Blob([fileBuffer], { type: 'text/markdown' });
  formData.append('file', blob, fileName);
  formData.append('extra', JSON.stringify({ obj_type: 'docx', file_extension: 'md' }));
  
  if (FOLDER_TOKEN) {
    formData.append('parent_token', FOLDER_TOKEN);
  }

  const res = await fetch('https://open-apis.feishu.cn/open-apis/drive/v1/medias/upload_all', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });

  const data = await res.json();
  if (data.code !== 0) {
    throw new Error(`Upload file failed: ${data.msg} (code: ${data.code})`);
  }
  return data.data.file_token;
}

async function createImportTask(token, fileToken, fileName) {
  const payload = {
    file_extension: 'md',
    file_token: fileToken,
    type: 'docx',
    file_name: fileName
  };
  
  if (FOLDER_TOKEN) {
    payload.point = {
      mount_key: 'explorer',
      mount_point: FOLDER_TOKEN
    };
  }

  const res = await fetch('https://open-apis.feishu.cn/open-apis/drive/v1/import_tasks', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });

  const data = await res.json();
  if (data.code !== 0) {
    throw new Error(`Create import task failed: ${data.msg} (code: ${data.code})`);
  }
  return data.data.ticket;
}

async function pollImportTask(token, ticket) {
  const maxRetries = 30;
  for (let i = 0; i < maxRetries; i++) {
    const res = await fetch(`https://open-apis.feishu.cn/open-apis/drive/v1/import_tasks/${ticket}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    const data = await res.json();
    if (data.code !== 0) {
      throw new Error(`Poll import task failed: ${data.msg} (code: ${data.code})`);
    }
    
    const job = data.data.result;
    if (job.job_status === 0) { // Success
      return job.token;
    } else if (job.job_status === 1 || job.job_status === 2) { // 1: Initial, 2: Processing
      await new Promise(r => setTimeout(r, 1000));
    } else {
      throw new Error(`Import job failed with status: ${job.job_status}`);
    }
  }
  throw new Error('Timeout waiting for import task completion');
}

async function main() {
  try {
    console.log('🔑 获取 Feishu Access Token...');
    const token = await getTenantAccessToken();
    console.log('✅ Access Token 获取成功。');

    for (const item of FILES_TO_SYNC) {
      console.log(`\n📄 开始上传: [${item.title}] (${item.localPath})`);
      const absPath = path.resolve(item.localPath);
      if (!fs.existsSync(absPath)) {
        console.warn(`⚠️ 本地文件不存在，跳过: ${absPath}`);
        continue;
      }
      
      const fileName = path.basename(absPath);
      
      console.log('📤 正在上传文件二进制流...');
      const fileToken = await uploadFile(token, absPath, fileName);
      console.log(`✅ 上传成功，File Token: ${fileToken}`);
      
      console.log('🔨 创建云文档导入任务...');
      const ticket = await createImportTask(token, fileToken, item.title);
      console.log(`✅ 导入任务已提交，Ticket: ${ticket}`);
      
      console.log('⏳ 正在转换并生成飞书云文档...');
      const docToken = await pollImportTask(token, ticket);
      const docUrl = `https://feishu.cn/docx/${docToken}`;
      console.log(`🎉 转换成功！飞书云文档链接:\n👉 ${docUrl}`);
    }
  } catch (err) {
    console.error('❌ 执行失败:', err.message);
  }
}

main();
