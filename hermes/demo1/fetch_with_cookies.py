import urllib.request, urllib.error, ssl, re, json, http.cookiejar

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Create cookie jar
cookie_jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(
    urllib.request.HTTPSHandler(context=ctx),
    urllib.request.HTTPCookieProcessor(cookie_jar)
)

headers = {
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

# First, visit zhihu.com to get cookies
print('Step 1: Getting cookies from zhihu.com...')
try:
    req = urllib.request.Request('https://www.zhihu.com/', data=None, headers=headers)
    resp = opener.open(req, timeout=15)
    text = resp.read().decode('utf-8', errors='replace')
    print(f'Got zhihu.com: {resp.status} {len(text)} chars')
    
    # Print cookies
    for c in cookie_jar:
        print(f'  Cookie: {c.name}={c.value[:20]}...')
except Exception as e:
    print(f'Error: {e}')

print('\nStep 2: Now trying the article...')
try:
    req = urllib.request.Request('https://zhuanlan.zhihu.com/p/4130861066', data=None, headers=headers)
    resp = opener.open(req, timeout=15)
    raw = resp.read()
    try:
        text = raw.decode('utf-8', errors='replace')
    except:
        import gzip
        text = gzip.decompress(raw).decode('utf-8', errors='replace')
    
    print(f'Article: {resp.status} {len(text)} chars')
    
    # Check for anti-scrape
    if 'zse_ck' in text:
        print('Anti-scrape page (zse_ck)')
    
    # Look for article title
    m = re.search(r'<title>(.*?)</title>', text)
    if m:
        print(f'Title tag: {m.group(1)}')
    
    m = re.search(r'property="og:title"[^>]*content="([^"]+)"', text)
    if m:
        print(f'OG Title: {m.group(1)}')
    
    m = re.search(r'property="og:description"[^>]*content="([^"]+)"', text)
    if m:
        print(f'OG Desc: {m.group(1)[:300]}')
    
    # Look for INITIAL_STATE
    matches = re.findall(r'window\.__INITIAL_STATE__\s*=\s*({.*?});</script>', text, re.DOTALL)
    if matches:
        print(f'INITIAL_STATE found ({len(matches[0])} chars)')
        data = json.loads(matches[0])
        # Find article content
        if 'article' in data:
            print('Has article key')
            print(json.dumps(data['article'], ensure_ascii=False, indent=2)[:1000])
        elif 'posts' in data:
            print('Has posts key')
            print(json.dumps(data['posts'], ensure_ascii=False, indent=2)[:1000])
        else:
            print(f'Keys: {list(data.keys())}')
            print(json.dumps(data, ensure_ascii=False, indent=2)[:2000])
    else:
        print('No INITIAL_STATE found')
        # Try to find any JSON-LD
        matches2 = re.findall(r'<script[^>]*type="application/ld\+json"[^>]*>(.*?)</script>', text, re.DOTALL)
        if matches2:
            print(f'JSON-LD found: {matches2[0][:500]}')
    
    # Save for inspection
    with open('F:\\cy\\hermes\\demo1\\zhihu_full.html', 'w', encoding='utf-8') as f:
        f.write(text)
    print('Saved to zhihu_full.html')
    
except Exception as e:
    print(f'Error: {type(e).__name__}: {e}')
