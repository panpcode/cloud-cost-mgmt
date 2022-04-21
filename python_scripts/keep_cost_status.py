from pymongo import MongoClient
from random import randint
import time
from datetime import date
import datetime
from time import gmtime, strftime

#PANP:
#days x 38.64
#hours x ( 38.64 / 24 ) (edited)

#GEMO:
#days x 46,4
#hours x ( 46,4 / 24 )

#SWGT:
#days x 41,69
#hours x ( 41,69 / 24 )



#Step 1: Connect to MongoDB - Note: Change connection string as needed
client = MongoClient("mongodb://panos:papathanas@localhost:27017/admin")
db=client.test
#Step 2: Get data
data = db.items.find({'status':"Start"},{"rg_name","start_time"})

costs={  "MS-1-Task1-EastUS" : 38.64,
         "MS-1-Task2-WestUS" : 46.4
      }



for item in data:
 t2=time.gmtime( int(item['start_time'])/1000 )
 time_str=strftime("%Y-%m-%d %H:%M:%S", t2)
 earlier = datetime.datetime.strptime(time_str, "%Y-%m-%d %H:%M:%S")
 now = datetime.datetime.now()
 diff = now - earlier
 diff_hours=diff.seconds/3600
 hour_cost=costs[item["rg_name"]]/24
 #print(diff.seconds)
 #print( costs[item["rg_name"]] )
 #print("RG_NAME : "+item['rg_name'])
 #print("Cost : "+str( round( hour_cost*diff_hours, 2) )+" $")
 cost='$'+str( round( hour_cost*diff_hours, 2) )
 #print(cost)
 #print(str(cost)+' $')
 #db.items.insert({'_id'=item)
 db.items.update_one({ "_id" : item['_id'] }, { "$set": { "cost": cost } })