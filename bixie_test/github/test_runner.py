__author__ = 'schaef'
import csv
import os
import os.path
import sys
import shutil

from subprocess import call

current_dir = os.path.dirname(os.path.realpath(__file__))

def clone_project(git_url, target_dir):
    os.chdir(target_dir)
    call(["git", "clone",  git_url])

def run_test(url, dir):
    #clone_project(url, dir)
    #now find all classpath files
    result = [os.path.join(dp, f) for dp, dn, file_names in os.walk(dir) for f in file_names if os.path.splitext(f)[0] == '.classpath']
    for s in result:
        print s


def main():
    with open('test.csv', 'r') as csvfile:
        line_reader = csv.reader(csvfile, delimiter=',', quotechar='"')
        first_round = True
        for row in line_reader:
            if first_round==True:
                first_round=False
                continue
            print "running test for ", row[0]
            #setup the test folder
            dir = "{0}/{1}".format(current_dir, "_temp")
            #if os.path.exists(dir):
            #    shutil.rmtree(dir)
            if not os.path.exists(dir):
                os.makedirs(dir)

            run_test(row[0], dir)
            #delete the test folder
            #shutil.rmtree(dir)

if __name__ == "__main__":
    main()
