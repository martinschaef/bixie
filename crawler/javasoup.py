from bs4 import BeautifulSoup
import os, sys
import json
from pprint import pprint

def exists(it):
  return (it is not None)

def clean_list(lst):
  return filter(exists, set(lst))

def null_if(tag):
  return (tag.name == u'code' and
          tag.string == 'null' and
          exists(tag.next_sibling) and
          tag.next_sibling.startswith(u' if'))

def parent_block(tag):
  parent_dl = tag.find_parent('dl')
  if not parent_dl:
    return None

  return_label = parent_dl.find(u'span')
  
  if not return_label or return_label.string!=u'Returns:':
    return None
  
  return parent_dl.parent

def find_method_name(tag):
  h4 = tag.find('h4')
  if exists(h4):
    return h4.string
  else:
    return None

def find_relevant_methods(soup):
  candidates = soup.find_all(null_if)  
  blocks = clean_list(map(parent_block, candidates))
  return clean_list(map(find_method_name, blocks))

def find_class_name(soup):
  return soup.find_all('ul', class_='inheritance')[-1].find('li').string

def find_package(soup):
  return soup.find_all('div', class_='subTitle')[-1].string

def find_name(soup):
  return soup.find('div', class_='header').find('h2', class_='title').string.split(' ')[-1]

def process_file(filename):
  with open(filename, 'r') as f:
    soup = BeautifulSoup(f.read(), 'html.parser')
    try:
      return (find_package(soup), find_name(soup), find_relevant_methods(soup))
    except (IndexError, AttributeError):
      print "Error in file %s" % filename
      return (None, None, None)

def filename_filter(fname):
  return '.html' in fname and \
    'package-' not in fname

def dirpath_filter(dirpath):
  return 'class-use' not in dirpath and \
    'doc-files' not in dirpath

def walk_javadoc():
  results = {}

  for dirpath, dirs, files in os.walk(sys.argv[1]):
    if not dirpath_filter(dirpath):
      continue
    for filename in files:
      if not filename_filter(filename):
        continue
      fname = os.path.join(dirpath, filename)      
      package, name, methods = process_file(fname)
      if methods:
        results[".".join([package,name])] = {"package":package, "class":name, "methods":methods}        
        #results[(package, name)] = methods
        print "%s.%s: %s" % (package, name, str(methods))

  return results

if __name__ == '__main__':
  data = walk_javadoc()
  pprint(data)
  with open('null_methods.json', 'w') as outfile:
    json.dump(data, outfile)

