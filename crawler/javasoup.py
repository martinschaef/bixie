from bs4 import BeautifulSoup
import os, sys
import json
import re
import traceback
from pprint import pprint

def exists(it):
  return (it is not None)

def clean_list(lst):
  return filter(exists, set(lst))

def null_if(tag):
  return (tag.string == 'null' and
          exists(tag.next_sibling) and
          tag.next_sibling.startswith(u' if'))

def parent_block(tag):  

  parent_dl = tag.find_parent('dl')  
  if not parent_dl:    
    return None  

  return_labels = parent_dl.find_all(u'span')
  found = False
  for return_label in return_labels:
    if return_label.string=="Returns:":
      found = True
      #pprint (vars(return_label))
      break 

  if found==True:
    pass
  else:
    return None

  #pprint(vars(parent_dl.parent))

  return parent_dl.parent

def find_method_name(tag):
  h4 = tag.find('h4')
  if exists(h4):
    return h4.string
  else:
    return None

def find_relevant_methods(soup):
  candidates = soup.find_all(null_if)
  #print candidates
  blocks = clean_list(map(parent_block, candidates))  
  return clean_list(map(find_method_name, blocks))

def find_package(soup):
  return soup.find_all('div', class_='subTitle')[-1].string

def find_name(soup):
   header_div = soup.find('div', class_='header')
   h2_title = header_div.find('h2', class_='title')
   h2_string = h2_title.strings.next()
   h2_string_clean = re.sub(r'<.*', '', h2_string)
   words = h2_string_clean.split(' ')
   return words[-1]

def process_file(filename):
  with open(filename, 'r') as f:
    soup = BeautifulSoup(f.read(), 'html.parser')
    try:
      result = (find_package(soup), find_name(soup), find_relevant_methods(soup))
      #print result
      return result
    except (IndexError, AttributeError) as e:
      #print "Error in file %s: %s" % (filename, e)
      #traceback.print_exc()
      return (None, None, None)

def filename_filter(fname):
  return '.html' in fname and \
    'package-' not in fname

def dirpath_filter(dirpath):
  return 'class-use' not in dirpath and \
    'doc-files' not in dirpath

def walk_javadoc():
  results = {}

  if os.path.isfile(sys.argv[1]):
    package, name, methods = process_file(sys.argv[1])
    if methods:
      results[".".join([package,name])] = {"package":package, "class":name, "methods":methods}
    return results

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
        print "%s.%s: %s" % (package, name, str(methods))

  return results

if __name__ == '__main__':
  data = walk_javadoc()
  pprint(data)
  with open('null_methods.json', 'w') as outfile:
    json.dump(data, outfile)

