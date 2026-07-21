import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Try to get the article via Google cache with proper params
url = 'https://webcache.googleusercontent.com/search?q=cache:zhuanlan.zhihu.com/p/4130861066&strip=1&vwsrc=0'
try:
    req = urllib.request.Request(url, data=None, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9',
    })
    resp = urllib.request.urlopen(req, timeout=15, context=ctx)
    text = resp.read().decode('utf-8', errors='replace')
    print(f'Status: {resp.status} Length: {len(text)}')
    
    # Try to find the cached URL
    m = re.search(r'href="(/search\?q=cache:[^"]+)"', text)
    if m:
        cache_url = 'https://webcache.googleusercontent.com' + m.group(1)
        print(f'Found cache URL: {cache_url}')
        # Follow it
        req2 = urllib.request.Request(cache_url, data=None, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        })
        resp2 = urllib.request.urlopen(req2, timeout=15, context=ctx)
        text2 = resp2.read().decode('utf-8', errors='replace')
        print(f'Cache content: {len(text2)} chars')
        
        # Extract the actual cached page content
        # Google cache wraps content in <div id="google-cache">
        m2 = re.search(r'<div[^>]*id="google-cache"[^>]*>(.*?)</div>\s*</div>\s*</body>', text2, re.DOTALL)
        if m2:
            print(f'Found google-cache div: {len(m2.group(1))} chars')
            print(m2.group(1)[:2000])
        else:
            # Try to find any meaningful content
            # Remove scripts
            cleaned = re.sub(r'<script[^>]*>.*?</script>', '', text2, flags=re.DOTALL)
            cleaned = re.sub(r'<style[^>]*>.*?</style>', '', cleaned, flags=re.DOTALL)
            # Find body content
            m3 = re.search(r'<body[^>]*>(.*?)</body>', cleaned, flags=re.DOTALL)
            if m3:
                body = m3.group(1)
                # Strip HTML tags
                text_content = re.sub(r'<[^>]+>', ' ', body)
                text_content = re.sub(r'\s+', ' ', text_content).strip()
                print(f'Body text: {text_content[:2000]}')
            else:
                print('No body found, printing first 2000 chars:')
                print(text2[:2000])
    else:
        print('No cache URL found')
        print(text[:1000])
except Exception as e:
    print(f'Error: {type(e).__name__}: {e}')
