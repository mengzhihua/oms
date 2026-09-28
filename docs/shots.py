import asyncio, os
from playwright.async_api import async_playwright

BASE = "http://localhost:5173"
OUT = os.path.join(os.path.dirname(__file__), "images")
PAGES = [
    ("login", "/login", None),
    ("dashboard", "/dashboard", None),
    ("order-list", "/order/list", None),
    ("order-detail", "/order/detail/SO20260928-0010", None),
    ("order-create", "/order/create", None),
    ("order-import", "/order/import", None),
    ("aftersale-list", "/aftersale/list", None),
    ("aftersale-detail", "/aftersale/detail/RT20260928-0002", None),
    ("refund-list", "/aftersale/refund", None),
    ("inventory-stock", "/inventory/stock", None),
    ("inventory-policy", "/inventory/policy", None),
    ("inventory-txn", "/inventory/txn", None),
    ("basic-product", "/basic/product", None),
    ("basic-bundle", "/basic/bundle", None),
    ("basic-shop", "/basic/shop", None),
    ("basic-warehouse", "/basic/warehouse", None),
    ("basic-routing", "/basic/routing", None),
    ("integration-log", "/integration/log", None),
    ("integration-openapi", "/integration/openapi", None),
    ("report", "/report", None),
    ("system-user", "/system/user", None),
    ("system-oplog", "/system/oplog", None),
]

async def main():
    os.makedirs(OUT, exist_ok=True)
    async with async_playwright() as p:
        browser = await p.chromium.connect_over_cdp("http://localhost:29229")
        ctx = browser.contexts[0]
        page = await ctx.new_page()
        await page.set_viewport_size({"width": 1440, "height": 900})
        await page.goto(BASE + "/login")
        await page.wait_for_timeout(1000)
        # login via API to get token, put into localStorage like the app does
        token = await page.evaluate("""async () => {
          const r = await fetch('/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:'admin',password:'admin123'})});
          const j = await r.json(); return j.data;
        }""")
        for name, path, _ in PAGES:
            if name == "login":
                await ctx.clear_cookies()
                await page.evaluate("localStorage.clear()")
                await page.goto(BASE + "/login")
                await page.wait_for_timeout(1200)
                await page.screenshot(path=f"{OUT}/{name}.png")
                await page.evaluate("(d)=>{localStorage.setItem('oms_token',d.token);localStorage.setItem('oms_user',JSON.stringify(d.user));}", token)
                continue
            await page.goto(BASE + path)
            await page.wait_for_load_state("networkidle")
            await page.wait_for_timeout(1500)
            await page.screenshot(path=f"{OUT}/{name}.png")
            print("shot", name)
        await page.close()

asyncio.run(main())
