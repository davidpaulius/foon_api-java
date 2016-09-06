import re
import os, sys
from nltk.stem import WordNetLemmatizer
wnl = WordNetLemmatizer()

# Create Structure that can contain, old identifier, new identifier, object name, filename, state, motion
# This class is used to create the records file.
class Record:
	def __init__(record, obj = "", oldID = 0, newID = 0, initState = "none", finalState = "none", fileName = "none", motion = ""):
		record.obj = obj
		record.oldID = oldID
		record.newID = newID
		record.initState = initState
		record.finalState = finalState
		record.fileName = fileName
		record.motion = motion


#Main object information for MainIndex
class MainInfo:
	def __init__(maininfo, id = 0, obj = "", state = "", motion = ""):
		maininfo.id = id
		maininfo.obj = object
		maininfo.state = state
		maininfo.motion = motion



done = False
recordList = []
mainList = []
stateList = []
motionList = []
firstOccurrence = True

# Re-populates the strMainList so we have the previous objects from other runs of the program. (Object List)
strMainList = open("Main Index.txt", "r").read().splitlines()
for x in strMainList:
	mainRecord = MainInfo()
	tempMainInfo = x.split("\t")
	mainRecord.obj = tempMainInfo[1]
	mainRecord.id = tempMainInfo[0]
	mainList.append(mainRecord)

# Re-populates the strStateList so we have the previous objects from other runs of the program. (State List)
strStateList = open("StateIndex.txt", "r").read().splitlines()
for x in strStateList:
	stateRecord = MainInfo()
	tempStateInfo = x.split("\t")
	stateRecord.state = tempStateInfo[1].rstrip()
	stateRecord.id = tempStateInfo[0]
	stateList.append(stateRecord)

# Re-populates the strMotionList so we have the previous objects from other runs of the program.  (Motion List)
strMotionList = open("motionindex.txt", "r").read().splitlines()
for x in strMotionList:
	motionRecord = MainInfo()
	tempMotionInfo = x.split("\t")
	motionRecord.motion = tempMotionInfo[1]
	motionRecord.id = tempMotionInfo[0]
	motionList.append(motionRecord)


# Populate oldRecords so we can re-write all information from the previous programs runtime.
strRecordList = open("Records.txt", "r").read().splitlines()
for x in strRecordList:
	oldRecord = Record()
	tempInfo = x.split("\t")
	oldRecord.obj = tempInfo[0]
	oldRecord.oldID = tempInfo[1]
	oldRecord.newID = tempInfo[2]
	oldRecord.initState = tempInfo[3]
	oldRecord.finalState = tempInfo[4]
	oldRecord.fileName = tempInfo[5]
	oldRecord.motion = tempInfo[6]
	recordList.append(oldRecord)

# Old implementation to find functional units.
# Using // to determine functional units is also implemented.
uniqItemCounter = 0
tempListCounter = 0
finishCounter = 0

# Used to find Exact duplicates.
uniqListObj = []
uniqListStates = []
uniqListState = []
uniqListObjMotion = []
oFound = False

# Used to check if there are duplicates or objects with same name.
objectChecker = ''
idChecker = ''
stateChecker = ''
motionChecker = ''


# Variables to set objects, states, motions to the same format.
strLowerState = ''
strLowerMotion = ''
strLowerObj = ''
strLowerRecord = ''
strLowerRecordFinalState = ''
strRemove = ''

plural1 = 'es'
plural2 = 'ies'
plural3 = 's'


# unlikely plural occurence plural4 = 'en'
# unlikely plural occurence plural5 = 'ren'

# Used to determine if a full functional unit has been recorded.
incompleteRecordList = []



# Populate the dictDuplicate with key objects.
dictDuplicate = {}

for x in mainList:
	dictDuplicate[x.obj] = list()


# I use a while loop to process all files that I currently have.

# -- Dave's notes:	changing the script to instead run through all files within directory.
#					this will save time in running through multiple files and correcting them.
file_name = raw_input("Enter directory name: ")
file_list = os.listdir(file_name); # using function to list all files within a certain folder.

for F in file_list:
	new_file_name = os.path.splitext(F)[0] + '-New.txt'
	print new_file_name
	newFile = open(os.path.join(file_name, new_file_name), 'w')
	#file_name += '.txt'

	# Leave the while loop if there are no more files.
	#if(file_name == '.txt'):
	#	break
	myFile = open(os.path.join(file_name, F), "r")

	# Iterate through FOON files to find objects, states, identifiers, and motions.
	for line in myFile:

		# Find Functional unit using //
		lineValue = re.match("/", line)
		if lineValue:
			#print "HEY THERE"
			# Write all the now completed temporary records.
			for x in incompleteRecordList:
				recordList.append(x)
			tempListCounter = 0
			finishCounter = 0
			incompleteRecordList = []
			firstOccurrence = True
			newFile.write("//\n")
			continue


		foundInMain = False
		# Find all of the O objects.
		object = re.match("O", line)
		if object:
			# Split the line into an identifier and an object.
			id = line.split("\t")
			print id;
			id[1] = id[1].lower().rstrip()

			# Creating the dictionary Keys (filled by the main objects list)
			# -- Dave's notes: 	id[2] will contain the motion identifier..
			#					Therefore, you must print everything back as needed.
			for x in mainList:
				#if id[1] == x.obj and id[1].find(x.obj) > -1:
				if id[1] == x.obj:
					objectChecker = x.obj
					idChecker = x.id
					newFile.write("O" + str(idChecker) + "\t" + x.obj + "\t" + id[2])
					print "O" + str(idChecker) + "\t" + x.obj + "\t" + id[2]
					foundInMain = True
					break


			#for x in mainList:
			#	if id[1].find(x.obj) > -1:
			#		break


			if(foundInMain == False):
				print id[1] + " else"
				newObject = raw_input("Enter new object: ")
				newElement = MainInfo()
				newElement.obj = newObject
				newElement.id = len(mainList) + 1
				mainList.append(newElement)
				newFile.write("O" + str(newElement.id) + "\t" + newElement.obj + "\t" + id[2])



			# If data was input incorrectly make sure the program continues.
			if id[1] == '':

				continue
			# Counters are used to find the correct information.
			if firstOccurrence:
				strLowerObj = id[1].lower().rstrip()
				id[1] = strLowerObj
				oFound = True
				aRecord = Record()
				aRecord.obj = id[1]
				aRecord.oldID = id[0]
				aRecord.fileName = file_name
				# Add all information that is currently available for Records
				incompleteRecordList.append(aRecord)
				continue


			else:
				continue

		state = re.match("S", line)
		if state:
			# Find the state
			findState = line.split("\t")
			findState[1] = findState[1].lower().rstrip()
			print "findState - ", findState
			for x in stateList:
				if findState[1] == x.state:
					stateChecker = x.state
					idChecker = x.id
					break

			#print stateChecker
			#print findState[1]

			# -- Dave's notes: will contain the list of ingredients bounded by {}..
			if stateChecker == findState[1]:
				newFile.write(line.replace(findState[0],"S" + idChecker))
			else:
				newFile.write(line)

			if findState[1] == '':
				continue

			# If it is the first occurrence input initial state, if not, input final state
			if firstOccurrence:
				if oFound != True:
					continue
				strLowerState = findState[1].lower().rstrip()
				findState[1] = strLowerState
				aRecord.initState = findState[1]
				incompleteRecordList[tempListCounter].initState = findState[1]
				tempListCounter += 1
				oFound = False

				continue

			else:

				strLowerState = findState[1].lower().rstrip()
				findState[1] = strLowerState
				incompleteRecordList[finishCounter].finalState = findState[1]
				if finishCounter != tempListCounter -1:
					finishCounter += 1
					continue
				else:
					# Write all the now completed temporary records.
					for x in incompleteRecordList:
						recordList.append(x)
				tempListCounter = 0
				finishCounter = 0
				incompleteRecordList = []
				firstOccurrence = True
				continue

		motion = re.match("M", line)
		if motion:

			# Find the motion
			findMotion = line.split("\t")
			print findMotion;
			findMotion[1] = findMotion[1].lower().rstrip('\n')

			strLowerMotion = findMotion[1].lower().rstrip()
			if(strLowerMotion.find("pick") > -1):
				findMotion[1] = "pick + place"

			if findMotion == '':
				print "Blank motion found!"
				raw_input("Press any key to continue..")

			for x in motionList:
				# David's notes: if findMotion[1] == x.motion or id[1].find(x.motion) > -1:
				if findMotion[1] == x.motion:
					motionChecker = x.motion
					idChecker = x.id
					foundInMain = True
					print "M" + str(idChecker) + "\t" + motionChecker + "\t" + findMotion[2] + "\t" + findMotion[3]
					newFile.write("M" + str(idChecker) + "\t" + motionChecker + "\t" + findMotion[2] + "\t" + findMotion[3])
					break

			#for x in motionList:
			#	if findMotion[1].find(x.motion) > -1:
			#		newFile.write("M" + str(idChecker) + "\t" + motionChecker + "\t" + findMotion[2] + "\t" + findMotion[3])
			#		foundInMain = True
			#		break

			if(foundInMain == False):
				print findMotion[1]
				newMotion = raw_input("Enter new motion: ")
				newElement = MainInfo()
				newElement.motion = newMotion
				newElement.id = len(motionList) + 1
				motionList.append(newElement)
				#print "HERE IS I HE IS"
				newFile.write("M" + str(newElement.id) + "\t" + newElement.motion + "\t" + str(findMotion[2]) + "\t" + str(findMotion[3]))
				print "M" + str(newElement.id) + "\t" + newElement.motion + "\t" + str(findMotion[2]) + "\t" + str(findMotion[3])

			if findMotion[1] == '':
				continue

			# Input the motion into the temporary record list.
			for x in incompleteRecordList:
				findMotion[1].lower().rstrip()
				x.motion = findMotion[1].lower().rstrip()
			firstOccurrence = False
			continue
	raw_input("Moving to next file...")
	newFile.close()
	myFile.close()

# Parse through the data and make sure there are no duplicates.
for x in mainList:
	foundFlag = False
	strLowerRecord = wnl.lemmatize(x.obj)
	x.obj = strLowerRecord
	for y in uniqListObj:
		if x.obj == y.obj:
			x.newID = y.newID
			foundFlag = True
			break

	if foundFlag:
		foundFlag = False

	else:
		uniqListObj.append(x)
		x.newID = uniqItemCounter
		uniqItemCounter += 1

# Write the information to the main index file.
indexMain = open("Main Index.txt", "w")
uniqItemCounter = 0
uniqStringForFile = ""
for x in uniqListObj:
	uniqStringForFile += (str(uniqItemCounter) + "\t"	+ x.obj + "\n")
	uniqItemCounter += 1
indexMain.write(uniqStringForFile)



# populate the duplicate dictionary
for key in dictDuplicate:
	for x in uniqListObj:
		if x == key or x.obj.find(key) > -1:
			dictDuplicate[key].append(x.obj)

#Used for debugging.
#for x in uniqListObj:
	#print x.obj


for x in recordList:
	strLowerRecord = x.obj.lower().rstrip()
	x.obj = strLowerRecord
	for y in mainList:
		if x.obj == y.obj:
			x.newID = y.id
			break



record = open("Records.txt", "w")
recordStringForFile = ""
for x in recordList:
	recordStringForFile += (x.obj + "\t" + str(x.oldID) + "\t" + str(x.newID) + "\t" + x.initState + "\t" + x.finalState + "\t" + x.fileName + "\t" + x.motion + "\n")
record.write(recordStringForFile)

uniqItemCounter = 0

# Parse through data to make sure there are no duplicates in the states.
for x in recordList:
	foundFlag = False
	strLowerRecord = x.initState.lower().rstrip()
	x.initState = strLowerRecord
	strLowerRecordFinalState = x.finalState.lower().rstrip()
	x.finalState = strLowerRecordFinalState
	for y in uniqListState:
		if x.initState == y.initState:
			x.newID = y.newID
			foundFlag = True
			break
	if foundFlag:
		foundFlag = False
	else:
		uniqListStates.append(x.initState)
		uniqListStates.append(x.finalState)
		x.newID = uniqItemCounter
		uniqItemCounter += 1






result = sorted(set(uniqListStates))

stateIndex = open("StateIndex.txt", "w")
uniqStringForFile = ""
uniqItemCounter = 0
for item in result:
	uniqStringForFile += (str(uniqItemCounter) + "\t" + item + "\n")
	uniqItemCounter += 1
stateIndex.write(uniqStringForFile)


# Write information to Records.txt

uniqItemCounter = 0
for x in recordList:
	foundFlag = False
	strLowerMotion = x.motion.lower().rstrip()
	if(strLowerMotion.find("pick") > -1):
		strLowerMotion = "pick + place"
	x.motion = strLowerMotion
	for y in uniqListObjMotion:
		if x.motion == y.motion:
			x.newID = y.newID
			foundFlag = True
			break

	if foundFlag:
		foundFlag = False

	else:
		uniqListObjMotion.append(x)
		x.newID = uniqItemCounter
		uniqItemCounter += 1



motionIndex = open("motionIndex.txt", "w")
uniqItemCounter = 0
uniqStringForFile = ""
for x in motionList:
	uniqStringForFile += (str(uniqItemCounter) + "\t"	+ x.motion + "\n")
	uniqItemCounter += 1
motionIndex.write(uniqStringForFile)


# Write the dictionary duplicates to the file

duplicateFile = open("duplicate.txt", "w")
uniqStringForFile = ""



for key in dictDuplicate:
	duplicateFile.write(str(key) + "\t" + " ".join(dictDuplicate[key]) + "\n")

duplicateFile.write(uniqStringForFile)

duplicateFile.close()




stateIndex.close()
motionIndex.close()
indexMain.close()
record.close()
