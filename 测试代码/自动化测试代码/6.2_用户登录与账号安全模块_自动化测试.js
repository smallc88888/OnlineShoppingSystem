// tests/auth_api_test.js
const mysql = require('mysql2/promise');

const BASE_URL = process.env.AUTH_BASE_URL || 'http://localhost:8081/api/users';

const dbConfig = {
    host: 'localhost',
    user: 'root',
    password: process.env.DB_PWD || process.env.MYSQL_PWD,
    database: 'shopping_system'
};

const colors = { green: '\x1b[32m', red: '\x1b[31m', yellow: '\x1b[33m', reset: '\x1b[0m' };

function assert(condition, message) {
    if (!condition) throw new Error(`[Assertion Failed] ${message}`);
}

async function runTests() {
    console.log(`${colors.yellow}开始执行Auth模块测试...${colors.reset}\n`);

    const randomUser = `test_bot_${Math.floor(Math.random() * 10000)}`;
    const password = 'BotPassword123@';
    const createdUsers = [randomUser];
    let dbConnection = null;

    if (!dbConfig.password) {
        throw new Error('请先通过 DB_PWD 或 MYSQL_PWD 环境变量提供本机 MySQL 密码');
    }

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
        // 用例 TC-REG-05: 密码长度少于8位
        // ==========================================
        console.log(`正在执行 [TC-REG-05] 密码长度少于8位测试...`);
        res = await fetch(`${BASE_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: `${randomUser}_p7`, password: 'Aa1234@', confirmPassword: 'Aa1234@' })
        });
        assert(res.status === 400, `预期状态码 400，实际为 ${res.status}`);
        console.log(`${colors.green}✔ [TC-REG-05] 测试通过！${colors.reset}`);

        // ==========================================
        // 用例 TC-REG-06: 密码长度8位边界
        // ==========================================
        console.log(`正在执行 [TC-REG-06] 密码长度8位边界测试...`);
        const password8User = `${randomUser}_p8`;
        createdUsers.push(password8User);
        res = await fetch(`${BASE_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: password8User, password: 'Aa12345@', confirmPassword: 'Aa12345@' })
        });
        assert(res.status === 200 || res.status === 201, `预期状态码 200/201，实际为 ${res.status}`);
        console.log(`${colors.green}✔ [TC-REG-06] 测试通过！${colors.reset}`);

        // ==========================================
        // 用例 TC-REG-07: 密码长度16位边界
        // ==========================================
        console.log(`正在执行 [TC-REG-07] 密码长度16位边界测试...`);
        const password16User = `${randomUser}_p16`;
        createdUsers.push(password16User);
        res = await fetch(`${BASE_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: password16User, password: 'Aa1234567890123@', confirmPassword: 'Aa1234567890123@' })
        });
        assert(res.status === 200 || res.status === 201, `预期状态码 200/201，实际为 ${res.status}`);
        console.log(`${colors.green}✔ [TC-REG-07] 测试通过！${colors.reset}`);

        // ==========================================
        // 用例 TC-REG-08: 密码长度超过16位
        // ==========================================
        console.log(`正在执行 [TC-REG-08] 密码长度超过16位测试...`);
        res = await fetch(`${BASE_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: `${randomUser}_p17`, password: 'Aa12345678901234@', confirmPassword: 'Aa12345678901234@' })
        });
        assert(res.status === 400, `预期状态码 400，实际为 ${res.status}`);
        console.log(`${colors.green}✔ [TC-REG-08] 测试通过！${colors.reset}`);

        // ==========================================
        // 用例 TC-REG-09: 密码四类字符复杂度校验
        // ==========================================
        console.log(`正在执行 [TC-REG-09] 密码四类字符复杂度测试...`);
        const weakPasswords = [
            { label: '缺少大写字母', value: 'aa123456@' },
            { label: '缺少小写字母', value: 'AA123456@' },
            { label: '缺少数字', value: 'Aabcdefg@' },
            { label: '缺少特殊字符', value: 'Aa1234567' }
        ];
        for (let i = 0; i < weakPasswords.length; i++) {
            const item = weakPasswords[i];
            res = await fetch(`${BASE_URL}/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: `${randomUser}_w${i}`, password: item.value, confirmPassword: item.value })
            });
            assert(res.status === 400, `${item.label}时预期状态码 400，实际为 ${res.status}`);
        }
        console.log(`${colors.green}✔ [TC-REG-09] 测试通过！${colors.reset}`);

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
            body: JSON.stringify({ username: randomUser, password: 'AnotherPassword123@', confirmPassword: 'AnotherPassword123@' })
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
        // 用例 TC-LOG-02/03: 密码错误与连续失败锁定边界
        // ==========================================
        console.log(`正在执行 [TC-LOG-02] 密码错误校验...`);
        console.log(`正在执行 [TC-LOG-03] 连续失败锁定边界测试...`);
        for (let i = 1; i <= 5; i++) {
            res = await fetch(`${BASE_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: randomUser, password: 'WrongPassword' })
            });
            if (i <= 4) {
                assert(res.status === 400, `第 ${i} 次尝试预期 400，实际为 ${res.status}`);
            } else {
                assert(res.status === 403, `第 ${i} 次尝试预期 403，实际为 ${res.status}`);
            }
        }
        const [lockRows] = await dbConnection.execute(
            `SELECT failed_attempts, locked_until FROM users WHERE username = ?`,
            [randomUser]
        );
        assert(lockRows[0].failed_attempts === 5, `预期失败次数为5，实际为 ${lockRows[0].failed_attempts}`);
        assert(lockRows[0].locked_until !== null, '预期第5次失败后写入 locked_until');
        console.log(`${colors.green}✔ [TC-LOG-02] 测试通过！${colors.reset}`);
        console.log(`${colors.green}✔ [TC-LOG-03] 测试通过！${colors.reset}`);

        // ==========================================
        // 用例 TC-LOG-06: 锁定期内正确密码仍禁止登录
        // ==========================================
        console.log(`正在执行 [TC-LOG-06] 锁定期内正确密码禁止登录...`);
        res = await fetch(`${BASE_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: randomUser, password: password })
        });
        assert(res.status === 403, `锁定期内正确密码登录预期 403，实际为 ${res.status}`);
        console.log(`${colors.green}✔ [TC-LOG-06] 测试通过！${colors.reset}`);

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
                const placeholders = createdUsers.map(() => '?').join(',');
                const [result] = await dbConnection.execute(
                    `DELETE FROM users WHERE username IN (${placeholders})`,
                    createdUsers
                );
                console.log(`${colors.green}✔ 已成功从数据库中物理抹除测试账号: ${createdUsers.join(', ')} (影响行数: ${result.affectedRows})${colors.reset}`);
            } catch (dbErr) {
                console.log(`${colors.red}❌ 数据清理失败，请手动前往数据库删除 ${createdUsers.join(', ')}。错误信息: ${dbErr.message}${colors.reset}`);
            } finally {
                await dbConnection.end(); // 安全关闭数据库连接
            }
        }
    }
}

runTests();
