#!/usr/bin/python3

import subprocess
import argparse
import json
from pymongo import MongoClient

parser = argparse.ArgumentParser()
parser.add_argument('serviceprincipalid', help='Azure service principal ID')
parser.add_argument('tenantid', help='Azure tenant ID')
parser.add_argument('key', help='Azure secret key')
parser.add_argument('subscriptionid', help='Azure subscription ID')

args = parser.parse_args()

def main():
    data_access = DataAccess()
    change_stream = data_access.db.items.watch([ {'$match': { 'operationType': {'$in': ['replace', 'update'] } } }])
    try:
        for change in change_stream:
            print('Event triggered : %s'%change)
            if 'fullDocument' in change:
                data_access.status_of_issue_changed(str(change['fullDocument']['_id']), change['fullDocument']['rg_name'],change['fullDocument']['status'])
            elif 'updateDescription' in change and 'status' in change['updateDescription']['updatedFields']:
                status = change['updateDescription']['updatedFields']['status']
                jira_id = str(change['documentKey']['_id'])
                rg = data_access.get_resource_group(jira_id)[0]['rg_name']
                data_access.status_of_issue_changed(jira_id, rg, status)
    except Exception as e:
        print("Something went wrong : "+str(e))

class DataAccess:

    def __init__(self, ip = 'localhost', port = '27017'):
         self.ip = ip
         self.port = port
         connection_string = 'mongodb://panos:papathanas@%s:%s/admin'%(ip,port)
         self.client = MongoClient(connection_string)
         self.db=self.client.test

    def status_of_issue_changed(self, jira_issue, resource_group,status):
        if status.lower() == 'stop':
            jira_issues_count = self.__get_jira_issues_count(resource_group)
            if jira_issues_count == 1:
                azure_client = AzureClient(args.serviceprincipalid,args.tenantid,args.key,args.subscriptionid)
                azure_client.stop(resource_group)
                azure_client.remove_tag(resource_group,'jira_issue_' + jira_issue)
            else:
                print('Jira issue ' +  jira_issue + ' shares the resource group with other issues')
        elif status.lower() == 'start':
            azure_client = AzureClient(args.serviceprincipalid,args.tenantid,args.key,args.subscriptionid)
            azure_client.start(resource_group)
            azure_client.add_tag(resource_group,'jira_issue_' + jira_issue, jira_issue)

    def get_resource_group(self,jira_issue):
        return self.db.items.find({'_id':jira_issue},{"rg_name"})

    def __get_jira_issues_count(self, resource_group):
        return self.db.items.find({'rg_name':resource_group},{"_id"}).count()

class AzureClient:

    def __init__(self, service_principal_id, tenant_id, secret_key, subscription_id):
         self.__login(service_principal_id, tenant_id,secret_key)
         self.__set_subscription_id(subscription_id)

    def __list_vms_in_rg_by_id(self, resource_group):
        return self.__az_cli('az vm list -g ' + resource_group + ' --query "[].id" -o tsv')[1]

    def start(self, resource_group):
        vms = self.__list_vms_in_rg_by_id(resource_group)
        return self.__az_cli('az vm start --ids ' + vms.replace("\n", " ") +' --no-wait')[1]

    def stop(self, resource_group):
        vms = self.__list_vms_in_rg_by_id(resource_group)
        return self.__az_cli('az vm stop --ids ' + vms.replace("\n", " ") +' --no-wait')[1]

    def add_tag(self, resource_group, tag_name, tag_value):
        return self.__az_cli('az group update -g ' + resource_group + " --set tags." + tag_name + "='" + tag_value + "'")[1]

    def remove_tag(self, resource_group, tag_name):
        return self.__az_cli('az group update -g ' + resource_group + " --remove tags." + tag_name)[1]

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