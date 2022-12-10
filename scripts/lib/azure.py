import re
import os

REGION = "westeurope"
RESOURCE_GROUP = "scc2223-rg-"+REGION+"-d464"
EXTERNAL_IP = "20.103.13.134"
TARGET = "http://"+EXTERNAL_IP+":80/rest"

