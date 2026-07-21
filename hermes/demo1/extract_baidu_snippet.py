import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Get Baidu search page and extract detailed snippet
search_url = 'https://www.baidu.com/s?wd=zhuanlan.zhihu.com%2Fp%2F4130861066'
req = urllib.request.Request(search_url, data=None, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9',
})
resp = urllib.request.urlopen(req, timeout=10, context=ctx)
text = resp.read().decode('utf-8', errors='replace')

# Save full page
with open('F:\\cy\\hermes\\demo1\\baidu_full.html', 'w', encoding='utf-8') as f:
    f.write(text)

# Try to find the result with zhihu
# Look for content-right divs
content_rights = re.findall(r'class="content-right_[^"]*"[^>]*>(.*?)</div>\s*</div>\s*</div>', text, re.DOTALL)
print(f'Found {len(content_rights)} content-right divs')

for i, cr in enumerate(content_rights):
    if 'zhuanlan' in cr or '封装' in cr or 'ISO' in cr:
        clean = re.sub(r'<[^>]+>', '\n', cr)
        clean = re.sub(r'\n{3,}', '\n\n', clean).strip()
        print(f'\n--- Content Right {i} ---')
        print(clean[:2000])

# Also try to find the result item
# Look for div with data-click containing the URL
results = re.findall(r'<div[^>]*class="result[^"]*c-container[^"]*"[^>]*data-click="([^"]*)"[^>]*>(.*?)</div>\s*</div>', text, re.DOTALL)
print(f'\nFound {len(results)} result containers')

for click_data, content in results:
    if 'zhuanlan.zhihu.com' in content or '4130861066' in click_data:
        clean = re.sub(r'<[^>]+>', '\n', content)
        clean = re.sub(r'\n{3,}', '\n\n', clean).strip()
        print(f'\n--- Result ---')
        print(clean[:3000])
        break

# Try to find any text content with "封装" or "ISO"
all_text = re.sub(r'<script[^>]*>.*?</script>', '', text, flags=re.DOTALL)
all_text = re.sub(r'<style[^>]*>.*?</style>', '', all_text, flags=re.DOTALL)
all_text = re.sub(r'<[^>]+>', '\n', all_text)
all_text = re.sub(r'\n{3,}', '\n\n', all_text)

# Find lines containing relevant keywords
lines = all_text.split('\n')
for i, line in enumerate(lines):
    line = line.strip()
    if line and ('封装' in line or 'ISO' in line or '镜像' in line or '重装' in line or 'Windows' in line):
        # Print context
        start = max(0, i-1)
        end = min(len(lines), i+3)
        print(f'\n[Line {i}]')
        for j in range(start, end):
            if lines[j].strip():
                print(f'  {lines[j].strip()[:200]}')
