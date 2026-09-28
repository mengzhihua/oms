"""将 项目汇报.md 渲染为自包含 HTML（截图内嵌 base64，Mermaid 由浏览器渲染）并导出 PDF。
用法: python3 docs/build.py   （PDF 导出需要本机 Chrome 开启 CDP 端口 29229，失败时删除旧 PDF 并以非 0 退出）
"""
import asyncio, base64, os, re, sys, urllib.request
import markdown

MERMAID_URL = "https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "项目汇报.md")
HTML_OUT = os.path.join(HERE, "项目汇报.html")
PDF_OUT = os.path.join(HERE, "项目汇报.pdf")

md = open(SRC, encoding="utf-8").read()
body = markdown.markdown(md, extensions=["tables", "fenced_code", "toc"])
# fenced mermaid -> <pre class="mermaid">
body = re.sub(r'<pre><code class="language-mermaid">(.*?)</code></pre>',
              lambda m: '<pre class="mermaid">' + m.group(1).replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&") + '</pre>',
              body, flags=re.S)

def inline(m):
    p = os.path.join(HERE, m.group(1))
    b = base64.b64encode(open(p, "rb").read()).decode()
    return f'src="data:image/png;base64,{b}"'
body = re.sub(r'src="(images/[^"]+)"', inline, body)

CSS = """
body{font-family:"Noto Sans CJK SC","PingFang SC","Microsoft YaHei",sans-serif;max-width:1080px;margin:0 auto;padding:32px 40px;color:#1f2d3d;line-height:1.7;font-size:15px}
h1{font-size:30px;border-bottom:3px solid #409eff;padding-bottom:10px;margin-top:0}
h2{font-size:22px;border-left:5px solid #409eff;padding-left:10px;margin-top:44px;page-break-after:avoid}
h3{font-size:17px;color:#303133;margin-top:28px}
table{border-collapse:collapse;width:100%;margin:12px 0;font-size:14px;page-break-inside:avoid}
th,td{border:1px solid #dcdfe6;padding:7px 10px;text-align:left}th{background:#f5f7fa}
tr:nth-child(even) td{background:#fafafa}
img{max-width:100%;border:1px solid #e4e7ed;border-radius:6px;box-shadow:0 2px 8px rgba(0,0,0,.08);margin:8px 0 18px;page-break-inside:avoid}
pre{background:#f6f8fa;padding:12px 14px;border-radius:6px;overflow:auto;font-size:13px}
pre.mermaid{background:#fff;text-align:center;page-break-inside:avoid}
code{background:#f0f2f5;padding:1px 5px;border-radius:3px;font-size:13px}pre code{background:none;padding:0}
blockquote{border-left:4px solid #e6a23c;background:#fdf6ec;margin:0;padding:6px 14px;color:#666}
hr{border:0;border-top:1px dashed #dcdfe6;margin:32px 0}
@media print{body{padding:0;max-width:none}pre.mermaid svg{max-height:180mm}}
"""


def mermaid_script():
    """内联 mermaid.js，使 HTML 离线可用；下载失败时回退为 CDN 引用。"""
    cache = os.path.join(HERE, ".mermaid.min.js")
    try:
        if not os.path.exists(cache):
            urllib.request.urlretrieve(MERMAID_URL, cache)
        return "<script>" + open(cache, encoding="utf-8").read() + "</script>"
    except Exception as e:  # noqa
        print("mermaid inline failed, fallback to CDN:", e)
        return f'<script src="{MERMAID_URL}"></script>'


html = f"""<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8"><title>OMS 订单管理系统 项目汇报</title>
<style>{CSS}</style>
{mermaid_script()}
<script>mermaid.initialize({{startOnLoad:true,theme:'default',securityLevel:'strict'}});</script>
</head><body>{body}</body></html>"""
open(HTML_OUT, "w", encoding="utf-8").write(html)
print("html ->", HTML_OUT, len(html) // 1024, "KB")


async def pdf():
    from playwright.async_api import async_playwright
    async with async_playwright() as p:
        browser = await p.chromium.connect_over_cdp("http://localhost:29229")
        page = await browser.contexts[0].new_page()
        await page.goto("file://" + HTML_OUT)
        await page.wait_for_function("document.querySelectorAll('pre.mermaid svg').length === document.querySelectorAll('pre.mermaid').length", timeout=30000)
        await page.wait_for_timeout(1000)
        await page.pdf(path=PDF_OUT, format="A4", print_background=True,
                       margin={"top": "16mm", "bottom": "16mm", "left": "12mm", "right": "12mm"})
        await page.close()
    print("pdf ->", PDF_OUT, os.path.getsize(PDF_OUT) // 1024, "KB")

if os.path.exists(PDF_OUT):
    os.remove(PDF_OUT)
try:
    asyncio.run(pdf())
except Exception as e:  # noqa
    print("pdf failed:", e)
    sys.exit(1)
