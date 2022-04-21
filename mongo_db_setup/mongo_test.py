import subprocess
import time
import json
import argparse
from pymongo import MongoClient

ip = 'localhost'
port = '27017'         
connection_string = 'mongodb://panos:papathanas@'+ip+':'+port+'/admin'
client = MongoClient(connection_string)
db=client.test