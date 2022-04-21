#!/usr/bin/python3

import subprocess
import time
import json
import argparse
from pymongo import MongoClient


parser = argparse.ArgumentParser()
parser.add_argument('serviceprincipalid', help='Azure service principal ID')
parser.add_argument('tenantid', help='Azure tenant ID')
parser.add_argument('key', help='Azure secret key')
parser.add_argument('subscriptionid', help='Azure subscription ID')

args = parser.parse_args()

def main():
    print('DBUpdater service is running')
    while True:
        data_access = DataAccess()
        data_access.update_db_with_resource_groups()
        time.sleep(900)

class DataAccess:

    def __init__(self, ip = 'localhost', port = '27017'):
         self.ip = ip
         self.port = port
         connection_string = 'mongodb://panos:papathanas@%s:%s/admin'%(ip,port)
         self.client = MongoClient(connection_string)
         self.db=self.client.test

    def update_db_with_resource_groups(self):
        azure_client = AzureClient(args.serviceprincipalid,args.tenantid,args.key,args.subscriptionid)
        resource_groups = azure_client.list_rgs()
        # Updating the shared MongoDB with all the resource groups from Azure portal
        collection = self.db["rgs"]
        collection.delete_many({})

        for rg in json.loads(resource_groups):
            print('Updating rgs collection with all the resource groups: %s \n'%rg['name'])
            new_entry = {"rg_name": rg['name'] }
            collection.insert_one(new_entry)

class AzureClient:

    def __init__(self, service_principal_id, tenant_id, secret_key, subscription_id):
         self.__login(service_principal_id, tenant_id,secret_key)
         self.__set_subscription_id(subscription_id)

    def list_rgs(self):
        return self.__az_cli('az group list')[1]

    def __login(self,service_principal_id,tenant_id, secret_key):
        return self.__az_cli('az login -u %s --service-principal --tenant %s -p %s'%(service_principal_id,tenant_id,secret_key))[1]

    def __set_subscription_id(self, subscription_id):
        return self.__az_cli('az account set --subscription  %s '%subscription_id)[1]

    def __az_cli(self, args, verbose=False):
        if verbose:
            print("Running cmd : " + args)
        create_app = subprocess.run(
            args, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        create_app_stdout = create_app.stdout.decode("utf-8")
        create_app_stderr = create_app.stderr.decode("utf-8")
        return [create_app_stderr, create_app_stdout]


if __name__ == '__main__':
    main()