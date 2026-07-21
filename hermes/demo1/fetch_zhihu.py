import urllib.request, urllib.error, ssl, re, gzip, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Try to get the article with full browser headers
req = urllib.request.Request(
    'https://zhuanlan.zhihu.com/p/4130861066',
    data=None,
    headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
        'Accept-Encoding': 'gzip, deflate',
        'Connection': 'keep-alive',
        'Upgrade-Insecure-Requests': '1',
        'Sec-Fetch-Dest': 'document',
        'Sec-Fetch-Mode': 'navigate',
        'Sec-Fetch-Site': 'none',
        'Sec-Fetch-User': '?1',
        'Cache-Control': 'max-age=0',
    }
)
try:
    resp = urllib.request.urlopen(req, timeout=15, context=ctx)
    raw = resp.read()
    try:
        text = raw.decode('utf-8', errors='replace')
    except:
        text = gzip.decompress(raw).decode('utf-8', errors='replace')

    print(f'Status: {resp.status} Length: {len(text)}')

    # Extract title
    m = re.search(r'<title>(.*?)</title>', text, re.DOTALL)
    if m:
        print(f'Title: {m.group(1)}')

    # Check for og data
    m = re.search(r'property="og:title"[^>]*content="([^"]+)"', text)
    if m:
        print(f'OG Title: {m.group(1)}')

    m = re.search(r'property="og:description"[^>]*content="([^"]+)"', text)
    if m:
        print(f'OG Desc: {m.group(1)[:200]}')

    # Check if anti-scrape
    if 'zse_ck' in text:
        print('Anti-scrape page detected (zse_ck)')

    # Try to find JSON data embedded in the page
    matches = re.findall(r'<script[^>]*>window\.__INITIAL_STATE__\s*=\s*({.*?});</script>', text, re.DOTALL)
    if matches:
        print(f'Found INITIAL_STATE JSON ({len(matches[0])} chars)')
        data = json.loads(matches[0])
        # Pretty print structure
        print(json.dumps(data, ensure_ascii=False, indent=2)[:2000])
    else:
        print('No INITIAL_STATE found')

    # Try to find any article content
    rich_text = re.search(r'class="Post-RichText[^"]*"[^>]*>(.*?)</div>', text, re.DOTALL)
    if rich_text:
        print(f'Found RichText content ({len(rich_text.group(1))} chars)')

    with open('F:\\cy\\hermes\\demo1\\zhihu_article.html', 'w', encoding='utf-8') as f:
        f.write(text)
    print('Saved to file')

except urllib.error.HTTPError as e:
    print(f'HTTP Error: {e.code} {e.reason}')
    body = e.read()
    try:
        print(body.decode('utf-8', errors='replace')[:500])
    except:
        print(f'Binary body, {len(body)} bytes')
except Exception as e:
    print(f'Error: {type(e).__name__}: {e}')
