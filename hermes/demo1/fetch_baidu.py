import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Try Baidu search
url = 'https://www.baidu.com/s?wd=zhuanlan.zhihu.com%2Fp%2F4130861066'
try:
    req = urllib.request.Request(url, data=None, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9',
    })
    resp = urllib.request.urlopen(req, timeout=10, context=ctx)
    text = resp.read().decode('utf-8', errors='replace')
    print(f'Baidu: {len(text)} chars')
    
    # Save
    with open('F:\\cy\\hermes\\demo1\\baidu_search.html', 'w', encoding='utf-8') as f:
        f.write(text)
    
    # Extract result titles
    results = re.findall(r'<h3[^>]*>(.*?)</h3>', text, re.DOTALL)
    for r in results[:5]:
        clean = re.sub(r'<[^>]+>', '', r).strip()
        print(f'  {clean}')
    
    # Check page title
    m = re.search(r'<title>(.*?)</title>', text)
    if m:
        print(f'Title: {m.group(1)}')
    
except Exception as e:
    print(f'Baidu Error: {type(e).__name__}: {e}')
