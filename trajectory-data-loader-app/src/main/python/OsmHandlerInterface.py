'''
Implements a Java interface (OsmHandler) to create 
a OsmHandler object from a Python class

@author: uqdalves, Lei Li
'''

import sys
from traminer.loader.map import OsmHandler as OsmHandlerJava
import InsertOsmData as iod
  
class OsmHandler(OsmHandlerJava):
    def __init__(self, host, port, dbName):
        # Create connection to MongoDB client 
        self.host = host
        self.port = port
        self.dbName = dbName
        self.uri = "mongodb://%s:%s" % (host, port)
        client = iod.MongoClient(self.uri)
        # get OsmHandler instance
        self.handler = iod.OsmHandler(client, self.dbName)
    
    def parse(self, fileName):
        self.fileName = fileName
        try:
            self.handler.parse(open(fileName))
            print "Parsing Successful!"
            # parsing successful 
            return True
        # catch any parser error
        except:
            e = sys.exc_info()[0]
            print "Parser Error: " + e
            return False
       
    def getHostName(self):
        return self.host
     
    def getPortNumber(self):
        return self.port 
        
    def getDatabaseName(self):
        return self.dbName

    def getMongoUri(self):
        return self.uri 
    
    def getParserResults(self):
        finalStatsString = "%d Nodes, %d Ways, %d Relations\n" % (self.handler.stat_nodes,
                                                                  self.handler.stat_ways,
                                                                  self.handler.stat_relations)
        return finalStatsString