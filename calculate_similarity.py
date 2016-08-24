from nltk.corpus import wordnet as wn
from nltk.corpus.reader.wordnet import WordNetError

# Prompting user for the name of the object new to FOON
# -- the name of the object may be something already in FOON; we are finding what other objects are similar to it as well
word = raw_input("Please input the word which is not found in FOON: > ");

# Opening and reading the file
# -- splitting each line into a list for ease of transition
_file = open('index.txt', 'r');
FOON_objects = _file.read().splitlines();
new_file = 'FOON_similarity_to_' + word + '.txt';
_file = open(new_file, 'w');

values = [];

# -- iterate through each item in the object index file
for i in range(len(FOON_objects)):
	# -- format for each line in file is the object number followed by the object name which needs to be split
	l = FOON_objects[i].split("\t");
	object_name = l[1]; # object name will be second item in the list

	try:
		# -- creating word objects with the name of the items
		word_1 = wn.synset(word + '.n.01');
		word_2 = wn.synset(object_name + '.n.01');
	except WordNetError:
		# -- WordNetError means that the object is not found in WordNet
		s = 0;
	else:
		# -- calculate Wu-Palmer metric for the words
		word_1.wup_similarity(word_2);
		s = word_1.wup_similarity(word_2);

	_file.write(object_name + "\t" + str(s) + "\n");
	print object_name + "\t" + str(s);
	values.append(s);

#endfor

#values.sort();
#for i in range(len(values)):
#	print values[i];
