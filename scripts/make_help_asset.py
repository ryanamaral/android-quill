#!/usr/bin/python


import os
from urllib import urlopen
from bs4 import BeautifulSoup

output_dir = os.path.abspath('../assets/help')
assert os.path.exists(output_dir)

files_list_images = []

files_list_html = [
    'Manual',
    'FAQ',
    'StylusSupport',
    'SupportedDevices',
    'Permissions' ]


def download_raw(url):
    data = urlopen(url).read()
    filename = url.split('/')[-1]
    with open(os.path.join(output_dir, filename), 'wb') as f:
        f.write(data)
    

def download_html(name):
    url = 'http://code.google.com/p/android-quill/wiki/'+name+'?show=content'
    data = urlopen(url).read()
    soup = BeautifulSoup(data)

    for script in soup.find_all('script'): 
        script.decompose()

    wiki_home = '/p/android-quill/wiki/'
    for link in soup.find_all('a'):
        target = link.get('href', None)
        if target is None:
            continue
        if target.startswith(wiki_home):
            target = target[len(wiki_home):]
            if target.count('#') == 0:
                target += '.html'
            elif target.count('#') == 1:
                target = target.replace('#', '.html#')
            else:
                raise ValueError('More than one pound-sign in link??')
            link['href'] = target

    for img in soup.find_all('img'):
        url = img['src']
        if not url.startswith('http://'):
            continue
        files_list_images.append(url)
        filename = url.split('/')[-1]
        img['src'] = filename

    print soup.prettify()

    with open(os.path.join(output_dir, name+'.html'), 'wb') as f:
        f.write(str(soup))




if __name__ == '__main__':
    for f in files_list_html:
        download_html(f)
    for f in files_list_images:
        download_raw(f)
