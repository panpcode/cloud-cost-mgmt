from pymongo import MongoClient
import pymongo 

client = MongoClient("mongodb://localhost:27017/")

def insertInCollection(db_name,collection_name,item):
    collection = client[db_name][collection_name]
    x = collection.insert_one(item)
    print(str(x))

def getCollection(db_name,collection,query=None):
    collection = client[db_name][collection]
    toreturn=[]
    if query is None:
        cursor = collection.find({})
    else:
        cursor = collection.find(query)
    for document in cursor:
        toreturn.append(document)
    return toreturn

def getDocument(db_name,collection,document_id):
    return getCollection(db_name,collection,{"_id":document_id})
    
def watchCollection(db_name, collection_name):
    stream = client[db_name][collection_name]
    pipeline = [{'$match': {'operationType': {'$in': ['insert','update','updateLookup','replace'] }}}]
    return stream.watch(pipeline)

def updateItem(db_name, collection_name, item , item_id=None):
    if item_id is None:
        item_id = item["_id"]
        item.pop('_id', None)
    client[db_name][collection_name].update_one({'_id':item_id}, {"$set": item}, upsert=False)
    item["_id"]=item_id

if __name__ == '__main__':
    
    insertInCollection("test","services_rg_queue",{ "resourceGroupName": "NGS-Test", "services":["ubuntu"], "creationStatus":"create" })
    print("All items ---")
    print(str(getCollection("test","services_rg_queue")))
    print("Not processed items ---")
    print(str(getCollection("test","services_rg_queue",{"creationStatus":"create"})))