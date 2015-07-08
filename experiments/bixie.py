import sys
import json
import tempfile
import os
import subprocess
import string
from contextlib import contextmanager

@contextmanager
def cd(newdir):
  prevdir = os.getcwd()
  os.chdir(newdir)
  try:
    yield
  finally:
    os.chdir(prevdir)

def mkdir(newdir):
  if not os.path.isdir(newdir):
    os.makedirs(newdir)

devnull = open(os.devnull, 'w')

def should_run_project(config, project):
  if 'only' in config:
    return project['name'] in config['only']
  if 'except' in config:
    return not (project['name'] in config['except'])
  return True

def run_project(project):
  if project['download']:
    download_project(project)

  with cd(project['name']):
    if project['build']:
      if not compile_project(project):
        return

    if project['analyze']:
      analyze_project(project)

def log(msg):
  sys.stderr.write(msg)
  sys.stderr.write("\n")


def download_git(project):
  if not os.path.isdir(project['name']):
    log("Downloading %s into %s" % (project['name'], project['working-dir']))
    subprocess.call(['git', 'clone', project['url'], project['name']], stdout=devnull, stderr=devnull)
  else:
    log("Already downloaded %s." % (project['name']))

  with cd(project['name']):
    log("Cleaning.")
    subprocess.call(['git', 'clean', '-df'], stdout=devnull, stderr=devnull)

    log("Checking out git-ref %s." % project['git-ref'])
    subprocess.call(['git', 'checkout', project['git-ref']], stdout=devnull, stderr=devnull)

def download_hg(project):
  if not os.path.isdir(project['name']):
    log("Downloading %s into %s" % (project['name'], project['working-dir']))
    subprocess.call(['hg', 'clone', project['url'], project['name']], stdout=devnull, stderr=devnull)
  else:
    log("Already downloaded %s." % (project['name']))

  with cd(project['name']):
    log("Cleaning.")
    subprocess.call('hg status -un | xargs -I {} rm {}', shell=True, stdout=devnull, stderr=devnull)

    log("Checking out hg-ref %s." % project['hg-ref'])
    subprocess.call(['hg', 'update', '-r', project['hg-ref'], '-C'], stdout=devnull, stderr=devnull)

def download_project(project):
  if 'git-ref' in project:
    download_git(project)
  elif 'hg-ref' in project:
    download_hg(project)

def compile_project(project):
  log("Compiling %s" % project['name'])
  for command in project['compile']:
    if subprocess.call(command, shell=True, stdout=devnull, stderr=devnull) != 0:
      log("Failed to compile %s. Oops." % project['name'])
      return False
  return True

def report_name(project, path):
  filename = "%s-%s" % (project['name'], string.replace(path, '/', '-'))
  return os.path.join(project['report-dir'], filename)

def analyze_project(project):
  log("Analyzing %s" % project['name'])

  for path in project['paths']:
    full_path = project['path-template'] % path
    report = report_name(project, path)   
    report_path = report + '.txt'
    log_path = report + '.log'
    error_path = report + '.err'
    log("Analyzing component %s at path %s" % (path, full_path))

    with open(log_path, 'w') as log_file, open(error_path, 'w') as err:
      subprocess.call(['java', '-jar', project['jar'], '-j', full_path, '-cp', full_path, '-o', report_path], stdout=log_file, stderr=err)

def main():
  file_name = 'bixie.json'
  if len(sys.argv)>1:
    file_name = sys.argv[1]

  f = open(file_name)
  j = json.loads(f.read())

  basedir = os.path.dirname(os.path.abspath(__file__))

  config = j['config']
  config['jar'] = os.path.join(basedir, config['jar'])
  config['working-dir'] = os.path.join(basedir, config['working-dir'])
  config['report-dir'] = os.path.join(basedir, config['report-dir'])

  mkdir(config['working-dir'])
  mkdir(config['report-dir'])

  for project in j['projects']:
    with cd(config['working-dir']):
      if should_run_project(config, project):
        proj = config.copy()
        proj.update(project)

        run_project(proj)

if __name__ == "__main__":    
  main()
