// tests/auth_api_test.js
const mysql = require('mysql2/promise');

const BASE_URL = 'http://localhost:8080/api/users';

const dbConfig = {
    host: 'localhost',
    user: 'root',
    password: '********', // 记得替换为真实的密码
    database: 'shopping_system'
};

const colors = { green: '\x1b[32m', red: '\x1b[31m', yellow: '\x1b[33m', reset: '\x1b[0m' };

function assert(condition, message) {
    if (!condition) throw new Error(`[Assertion Failed] ${message}`);
}

async function runTests() {
    console.log(`${colors.yellow}开始执行Auth模块测试...${colors.reset}\n`);

    const randomUser = `test_bot_${Math.floor(Math.random() * 10000)}`;
    const password = 'BotPassword123!';
    let dbConnection = null;

    try {
        // 将数据库连接提前，为了方便我们在测试中途直接操纵数据（如模拟时间流逝）
        dbConnection = await mysql.createConnection(dbConfig);

        // ==========================================
        // 用例 TC-REG-03: 用户名长度越界
        // ==========================================
        console.log(`正在执行 [TC-REG-03] 用户名长度越界测试...`);
        const overLimitUser = 'a'.repeat(21); // 构造 21 位的超长用户名
        let res = await fetch(`${BASE_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: overLimitUser, password: password, confirmPassword: password })
        });
        assert(res.status === 400, `预期状态码 400，实际为 ${res.status}`);
        console.log(`${colors.green}✔ [TC-REG-03] 测试通过！${colors.reset}`);

        // ==========================================
        // 用例 TC-REG-04: 核心字段判空防御
        // ==========================================
        console.log(`正在执行 [TC-REG-04] 核心字段判空防御测试...`);
        res = await fetch(`${BASE_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ password: password, confirmPassword: password }) // 故意漏掉 username
        });
        assert(res.status === 400, `预期状态码 400，实际为 ${res.status}`);
        console.log(`${colors.green}✔ [TC-REG-04] 测试通过！${colors.reset}`);

        // ==========================================
        // 正常测试流：创建基准测试用户 (TC-REG-01, TC-REG-02, TC-LOG-01)
        // ==========================================
        console.log(`正在执行 [TC-REG-01] 正常注册新用户...`);
        res = await fetch(`${BASE_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: randomUser, password: password, confirmPassword: password })
        });
        assert(res.status === 200 || res.status === 201, `预期状态码 200/201，实际为 ${res.status}`);
        console.log(`${colors.green}✔ [TC-REG-01] 测试通过！${colors.reset}`);

        console.log(`正在执行 [TC-REG-02] 拦截重复注册...`);
        res = await fetch(`${BASE_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: randomUser, password: 'AnotherPassword', confirmPassword: 'AnotherPassword' })
        });
        assert(res.status === 400 || res.status === 409, `预期状态码 400/409，实际为 ${res.status}`);
        console.log(`${colors.green}✔ [TC-REG-02] 测试通过！${colors.reset}`);

        console.log(`正在执行 [TC-LOG-01] 正常账号密码登录...`);
        res = await fetch(`${BASE_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: randomUser, password: password })
        });
        assert(res.status === 200, `预期状态码 200，实际为 ${res.status}`);
        console.log(`${colors.green}✔ [TC-LOG-01] 测试通过！${colors.reset}`);

        // ==========================================
        // 用例 TC-LOG-05: SQL注入防御测试
        // ==========================================
        console.log(`正在执行 [TC-LOG-05] SQL注入防御测试...`);
        res = await fetch(`${BASE_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            // 经典的万能密码注入语句
            body: JSON.stringify({ username: "' OR '1'='1", password: 'AnyPassword' })
        });
        assert(res.status === 400, `预期状态码 400，实际为 ${res.status}`);
        const errText = await res.text();
        // 如果后端报错提示包含“用户不存在”或类似校验失败的信息，证明注入被挡在了门外
        assert(errText.includes('用户不存在') || errText.includes('用户名或密码错误'), '返回错误信息异常');
        console.log(`${colors.green}✔ [TC-LOG-05] 测试通过！${colors.reset}`);

        // ==========================================
        // 用例 TC-LOG-02: 密码错误校验 (第 1 次失败)
        // ==========================================
        console.log(`正在执行 [TC-LOG-02] 密码错误校验...`);
        res = await fetch(`${BASE_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: randomUser, password: 'WrongPassword' })
        });
        assert(res.status === 400, `预期状态码 400，实际为 ${res.status}`);
        console.log(`${colors.green}✔ [TC-LOG-02] 测试通过！${colors.reset}`);

        // ==========================================
        // 用例 TC-LOG-03: 账号防爆破锁定测试 (继续后续失败尝试)
        // ==========================================
        console.log(`正在执行 [TC-LOG-03] 账号防爆破锁定测试...`);
        // 上面 TC-LOG-02 已经失败了 1 次，所以接着循环第 2 到第 6 次
        for (let i = 2; i <= 6; i++) {
            res = await fetch(`${BASE_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: randomUser, password: 'WrongPassword' })
            });
            // 之前的边界修正：第 5 次及以后会触发锁定返回 403
            if (i <= 4) {
                assert(res.status === 400, `第 ${i} 次尝试预期 400，实际为 ${res.status}`);
            } else {
                assert(res.status === 403, `第 ${i} 次尝试预期 403，实际为 ${res.status}`);
            }
        }
        console.log(`${colors.green}✔ [TC-LOG-03] 测试通过！${colors.reset}`);

        // ==========================================
        // 用例 TC-LOG-04: 锁定倒计时自动解封
        // ==========================================
        console.log(`正在执行 [TC-LOG-04] 锁定倒计时自动解封...`);
        // 【核心魔法】直接通过 SQL 语句，将该用户的锁定时间改到 1 分钟前（模拟 15 分钟已过）
        await dbConnection.execute(
            `UPDATE users SET locked_until = DATE_SUB(NOW(), INTERVAL 1 MINUTE) WHERE username = ?`,
            [randomUser]
        );
        console.log(`已通过数据库层模拟锁定时间过期...`);

        // 使用正确的密码重新登录
        res = await fetch(`${BASE_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: randomUser, password: password })
        });
        assert(res.status === 200, `模拟解封后登录预期 200，实际为 ${res.status}`);

        // 验证数据库里的 failed_attempts 是否被归零
        const [rows] = await dbConnection.execute(
            `SELECT failed_attempts FROM users WHERE username = ?`, [randomUser]
        );
        assert(rows[0].failed_attempts === 0, `预期登录成功后 failed_attempts 清零，实际为 ${rows[0].failed_attempts}`);
        console.log(`${colors.green}✔ [TC-LOG-04] 测试通过！${colors.reset}`);

        console.log(`\n${colors.green}🎉 所有测试套件执行完毕!${colors.reset}`);

    } catch (error) {
        console.log(`\n${colors.red}❌ 测试失败: ${error.message}${colors.reset}`);
    } finally {
        // ==========================================
        // Teardown: 物理毁灭测试产生的脏数据
        // ==========================================
        console.log(`\n${colors.yellow}🧹 正在启动 Teardown 清理程序...${colors.reset}`);
        if (dbConnection) {
            try {
                const [result] = await dbConnection.execute(
                    'DELETE FROM users WHERE username = ?',
                    [randomUser]
                );
                console.log(`${colors.green}✔ 已成功从数据库中物理抹除测试账号: ${randomUser} (影响行数: ${result.affectedRows})${colors.reset}`);
            } catch (dbErr) {
                console.log(`${colors.red}❌ 数据清理失败，请手动前往数据库删除 ${randomUser}。错误信息: ${dbErr.message}${colors.reset}`);
            } finally {
                await dbConnection.end(); // 安全关闭数据库连接
            }
        }
    }
}

runTests();