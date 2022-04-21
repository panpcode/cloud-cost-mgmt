from PyMongoHelper import *
import networkx as nx
import matplotlib.pyplot as plt
import subprocess as subproc

dependency_dag = nx.DiGraph()

def deploy_command(rg_name, service):
    extra_vars = '--extra-vars "service_name={} jira_item={}"'.format(service, rg_name)
    command = "bash deploy.sh"
    print('Running command:', command)
    try:
        subproc.check_call(command, shell=True)
        return True
    except subproc.CalledProcessError:
        return False

def deployRG(rg_name, services, rg_owner=None):
    dependencies_to_deploy = {}
    to_be_processed = []
    for service in services:
        to_be_processed.append(service)

    while to_be_processed:
        currently_processing = to_be_processed.pop(0).lower()
        if currently_processing in dependencies_to_deploy:
            continue
        else:
            dependencies_to_deploy[currently_processing]=True
            for successor in dependency_dag.successors(currently_processing):
                to_be_processed.append(successor.lower())
    print("Final dependencies for rg : "+rg_name+" - "+str(dependencies_to_deploy))

    #deployment
    dependencies_to_deploy = list(dependencies_to_deploy.keys())
    deployed_dependencies = []
    while dependencies_to_deploy:
        currently_deploying = dependencies_to_deploy.pop(0).lower()
        ok_to_deploy=True
        for successor in dependency_dag.successors(currently_deploying):
            if successor.lower() not in deployed_dependencies:
                ok_to_deploy=False
                break

        if ok_to_deploy:
            to_deploy = currently_deploying
            if to_deploy == 'new rg':
                to_deploy = 'newrg'
            elif to_deploy == 'ubuntu node':
                to_deploy = 'ubuntu'

            rg_name_deploy = rg_name.lower().replace('-', '')

            # Run the deployment command
            result = deploy_command(rg_name_deploy, to_deploy)
            print('Deployment result:', result)
            deployed_dependencies.append(currently_deploying)

        else:
            dependencies_to_deploy.append(currently_deploying)

def visualizeDependencyGraph():
    nx.draw_circular(dependency_dag, with_labels=True)
    plt.show()

def preprocessing():
    deployable_services = getCollection("test","available_services")
    
    for service in deployable_services:
        # script that can deploy the service

        # dependencies of the service
        dependency_dag.add_node(service["serviceName"].lower())
        if "requires" in service:
            print("Dependencies for service : "+service["serviceName"].lower() +" : "+ str(service["requires"]).lower())
            for dependency in service["requires"]:
                dependency_dag.add_edge(service["serviceName"].lower(),dependency.lower())
        else:
            print("No dependencies for service : "+service["serviceName"])

    # visualizeDependencyGraph()

preprocessing()

if __name__ == '__main__':
    while True:
        #unprocessed_items = getCollection("test","services_rg_queue",{"creationStatus":"create"})
        unprocessed_items = watchCollection("test","services_rg_queue")
        print("Watching collection services_rg_queue in test db")
        for item in unprocessed_items:
            if "fullDocument" not in item:
                item = getDocument("test","services_rg_queue",item["documentKey"]["_id"])[0]
            else:
                item = item["fullDocument"]
            print("Received an update event : "+str(item))
            if "creationStatus" not in item or item["creationStatus"] != "create":
                continue

            print("Processing : "+str(item))
            item["creationStatus"]="inprogress"
            updateItem("test","services_rg_queue",item)
            
            # do stuff here
            try :
            #if True:
                rg_owner = None
                if "assignee" in item:
                    rg_owner = item["assignee"]
                deployRG( item["resourceGroupName"], item["serviceName"], rg_owner)
                item["creationStatus"]="done"
                updateItem("test","services_rg_queue",item)
            except Exception as e:
                traceback.print_exc()
                print("Deployment of item : "+str(item)+" failed because of : "+str(e))
        print("Unprocessed items after ---")
        unprocessed_items = getCollection("test","services_rg_queue",{"creationStatus":"create"})
        for item in unprocessed_items:
            print("Not yet processed : "+str(item))
