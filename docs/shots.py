"""批量截取 OMS 各页面截图到 docs/images/。
前置: 前后端已启动(5173/8080)，本机 Chrome 开启 CDP 端口 29229。
环境变量: OMS_ADMIN_USER(默认 admin) / OMS_ADMIN_PASSWORD(必填)。
订单/售后详情页取列表中最新一条，库中无数据时跳过。
"""
import asyncio, os, sys
from playwright.async_api import async_playwright

BASE = "http://localhost:5173"
OUT = os.path.join(os.path.dirname(__file__), "images")
USER = os.environ.get("OMS_ADMIN_USER", "admin")
PASSWORD = os.environ.get("OMS_ADMIN_PASSWORD")
PAGES = [
    ("dashboard", "/dashboard"),
    ("order-list", "/order/list"),
    ("order-detail", "/order/detail/{order}"),
    ("order-create", "/order/create"),
    ("order-import", "/order/import"),
    ("aftersale-list", "/aftersale/list"),
    ("aftersale-detail", "/aftersale/detail/{return}"),
    ("refund-list", "/aftersale/refund"),
    ("inventory-stock", "/inventory/stock"),
    ("inventory-policy", "/inventory/policy"),
    ("inventory-txn", "/inventory/txn"),
    ("basic-product", "/basic/product"),
    ("basic-bundle", "/basic/bundle"),
    ("basic-shop", "/basic/shop"),
    ("basic-warehouse", "/basic/warehouse"),
    ("basic-routing", "/basic/routing"),
    ("integration-log", "/integration/log"),
    ("integration-openapi", "/integration/openapi"),
    ("report", "/report"),
    ("system-user", "/system/user"),
    ("system-oplog", "/system/oplog"),
]

API_JS = """async ([path, token]) => {
  const r = await fetch('/api' + path, {headers: token ? {Authorization: 'Bearer ' + token} : {}});
  return await r.json();
}"""


async def main():
    if not PASSWORD:
        sys.exit("请设置 OMS_ADMIN_PASSWORD")
    os.makedirs(OUT, exist_ok=True)
    async with async_playwright() as p:
        browser = await p.chromium.connect_over_cdp("http://localhost:29229")
        ctx = browser.contexts[0]
        page = await ctx.new_page()
        await page.set_viewport_size({"width": 1440, "height": 900})

        await page.goto(BASE + "/login")
        await page.evaluate("localStorage.clear()")
        await page.reload()
        await page.wait_for_selector("form", timeout=15000)
        await page.wait_for_timeout(800)
        await page.screenshot(path=f"{OUT}/login.png")
        print("shot login")

        login = await page.evaluate("""async ([u, p]) => {
          const r = await fetch('/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:u,password:p})});
          return await r.json();
        }""", [USER, PASSWORD])
        if login.get("code") != 0:
            sys.exit(f"登录失败: {login}")
        token = login["data"]["token"]
        await page.evaluate("(d)=>{localStorage.setItem('oms_token',d.token);localStorage.setItem('oms_user',JSON.stringify(d.user));}", login["data"])

        orders = await page.evaluate(API_JS, ["/order/page?size=1", token])
        returns = await page.evaluate(API_JS, ["/aftersale/page?size=1", token])
        ids = {
            "order": next((o["orderNo"] for o in orders.get("data", {}).get("records", [])), None),
            "return": next((r["returnNo"] for r in returns.get("data", {}).get("records", [])), None),
        }

        for name, path in PAGES:
            key = path[path.find("{") + 1:path.find("}")] if "{" in path else None
            if key and not ids[key]:
                print("skip", name, "(无数据)")
                continue
            await page.goto(BASE + path.format(**ids))
            await page.wait_for_load_state("networkidle")
            await page.wait_for_selector(".el-main .page, .el-main .card, .el-main .el-table", timeout=15000)
            await page.wait_for_timeout(1200)
            if "/login" in page.url:
                sys.exit(f"{name}: 被重定向到登录页")
            await page.screenshot(path=f"{OUT}/{name}.png")
            print("shot", name)
        await page.close()


asyncio.run(main())
