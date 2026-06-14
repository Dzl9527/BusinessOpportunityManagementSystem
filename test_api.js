async function runTests() {
  const backendUrl = 'http://localhost:8080';
  console.log('=== 开始 API 功能接口测试 ===\n');

  let token = '';
  let testOppId = null;

  // 1. 登录 (企业微信扫码或单点登录模拟)
  try {
    console.log('[1/8] 测试: 模拟登录 (WeCom Auth)...');
    const res = await fetch(`${backendUrl}/api/wecom/auth?code=mock_code`);
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    const data = await res.json();
    if (data && data.token) {
      token = data.token;
      console.log(`  ✅ 成功: 登录成功，Token获取成功，用户ID: ${data.userId}, 用户姓名: ${data.name}, 角色: ${data.role}`);
    } else {
      throw new Error('返回数据中未包含token');
    }
  } catch (err) {
    console.error(`  ❌ 失败: ${err.message}`);
    process.exit(1);
  }

  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };

  // 2. 仪表盘指标
  try {
    console.log('[2/8] 测试: 仪表盘指标 (/api/opportunities/metrics)...');
    const res = await fetch(`${backendUrl}/api/opportunities/metrics`, { headers });
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    const data = await res.json();
    console.log(`  ✅ 成功: pipeline总额 = ${data.totalPipeline}, 活跃商机数 = ${data.activeCount}`);
  } catch (err) {
    console.error(`  ❌ 失败: ${err.message}`);
  }

  // 3. 商机列表
  try {
    console.log('[3/8] 测试: 商机列表 (/api/opportunities)...');
    const url = `${backendUrl}/api/opportunities?userId=zhang_jingli&userName=${encodeURIComponent('张经理')}`;
    const res = await fetch(url, { headers });
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    const data = await res.json();
    console.log(`  ✅ 成功: 获取到 ${Array.isArray(data) ? data.length : 0} 个商机`);
  } catch (err) {
    console.error(`  ❌ 失败: ${err.message}`);
  }

  // 4. 商机下拉选项
  try {
    console.log('[4/8] 测试: 商机选项 (/api/opportunities/options)...');
    const res = await fetch(`${backendUrl}/api/opportunities/options`, { headers });
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    const data = await res.json();
    console.log(`  ✅ 成功: 选项获取成功，包含字段: ${Object.keys(data).join(', ')}`);
  } catch (err) {
    console.error(`  ❌ 失败: ${err.message}`);
  }

  // 5. 图表数据
  try {
    console.log('[5/8] 测试: 商机图表数据 (/api/opportunities/charts/stages)...');
    const res = await fetch(`${backendUrl}/api/opportunities/charts/stages`, { headers });
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    const data = await res.json();
    console.log(`  ✅ 成功: 获取到 ${data.length} 组图表数据`);
  } catch (err) {
    console.error(`  ❌ 失败: ${err.message}`);
  }

  // 6. 创建商机
  try {
    console.log('[6/8] 测试: 创建商机 (POST /api/opportunities)...');
    const newOpp = {
      name: '测试商机_' + Date.now().toString().slice(-6),
      clientName: '测试客户有限公司',
      stage: 'stage_discover', // 对应选项里的商机阶段
      estimateAmount: 500000.0,
      closeDate: '2026-12-31',
      deptId: 'device_dept', // 事业群/部门
      productLine: 'hardware',
      userId: 'zhang_jingli',
      userName: '张经理'
    };
    const res = await fetch(`${backendUrl}/api/opportunities`, {
      method: 'POST',
      headers,
      body: JSON.stringify(newOpp)
    });
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    const data = await res.json();
    testOppId = data.id;
    console.log(`  ✅ 成功: 创建商机成功，ID = ${testOppId}, 名称 = ${data.name}`);
  } catch (err) {
    console.error(`  ❌ 失败: ${err.message}`);
  }

  // 7. 更新商机阶段
  if (testOppId) {
    try {
      console.log(`[7/8] 测试: 更新商机阶段 (PUT /api/opportunities/${testOppId})...`);
      const res = await fetch(`${backendUrl}/api/opportunities/${testOppId}`, {
        method: 'PUT',
        headers,
        body: JSON.stringify({
          stage: 'stage_negotiate',
          userId: 'zhang_jingli',
          userName: '张经理'
        })
      });
      if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
      const data = await res.json();
      console.log(`  ✅ 成功: 更新商机阶段成功，新阶段 = ${data.stage}`);
    } catch (err) {
      console.error(`  ❌ 失败: ${err.message}`);
    }
  } else {
    console.log('[7/8] 跳过: 未能获取测试商机ID');
  }

  // 8. 获取当前用户信息
  try {
    console.log('[8/8] 测试: 当前登录用户信息 (/api/users/me)...');
    const res = await fetch(`${backendUrl}/api/users/me`, { headers });
    if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
    const data = await res.json();
    console.log(`  ✅ 成功: 用户ID = ${data.userId || data.wecomUserId}, 姓名 = ${data.name || data.userName}, 角色 = ${data.role}`);
  } catch (err) {
    console.error(`  ❌ 失败: ${err.message}`);
  }

  console.log('\n=== API 功能接口测试结束 ===');
}

runTests();
