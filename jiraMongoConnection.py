import pymongo

# create a client for the connection to the mongo databse that is running locally
client = pymongo.MongoClient("mongodb://localhost:27017/")
dbnames_list = client.list_database_names()
print("The following " + str(len(dbnames_list)) +  " databases exist:")
print(str(dbnames_list) + "\n")
if 'mscdb' in dbnames_list:
    print("The 'mscdb' database exists\n")
else:
    print("You have to create the 'mscdb' database first")
    exit()

# working on a new 'IssueTrackerJira' db
mydb = client["IssueTrackerJira"]

# checking the available collections
# list_of_collections = mydb.list_collection_names()
print("The available collections are:")
print(mydb.list_collection_names())