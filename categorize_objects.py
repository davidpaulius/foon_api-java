import sys;
# -- NLTK modules needed
from nltk.corpus import wordnet as wn
from nltk.corpus.reader.wordnet import WordNetError
from nltk.corpus import wordnet_ic

# Opening and reading the file
# -- splitting each line into a list for ease of transition
_file = open('Main Index.txt', 'r');
FOON_objects = _file.read().splitlines();

categories = [ 	['ingredient', '.n.03'],
				['condiment', '.n.01'],
				['spice', '.n.02'],
				['meat', '.n.01'],
				['container', '.n.01'],
				['powdered', '.a.01'],
				['leafy vegetable', '.n.01'],
				['oil', '.n.04'],
				['citrus', '.n.01'],
				['cutter', '.n.06'],
				['eating utensil', '.n.01'],
				['cutlery', '.n.01'],
				['liquid', '.n.01'],
				['juice', '.n.01'],
				['fruit', '.n.01'],
				['vegetable', '.n.01'],
				['seasoning', '.n.01'],
				['seafood', '.n.01'],
				['dairy product', '.n.01'] ];

new_file = 'object_categories.txt';
_file = open(new_file, 'w');

for i in range(len(categories)):
	_file.write(categories[i][0] + ":");
	for j in range(len(FOON_objects)):
		# -- iterate through each item in the object index file

		if FOON_objects[j].startswith("//"):
			continue;

		word = FOON_objects[j].split("\t")[1];
		word = word.replace(" ", "_");

		try:
			# -- creating word objects with the name of the items
			# 		- need to enforce and directly indicate WHICH synset definition (sense) should be used for certain words, as the first sense is not always food-related.
			if len(FOON_objects[j].split("\t")) == 2:
				word_1 = wn.synset(word + '.n.01');
			else:
				word_1 = wn.synset(word + '.n.0' + FOON_objects[j].split("\t")[2]);
			#endif
			word_2 = wn.synset(categories[i][0].replace(" ", "_") + categories[i][1]);
		except WordNetError:
			# -- WordNetError means that the object is not found in WordNet
			s = 0;
		else:
			# -- calculate Wu-Palmer metric for the words (for ALL senses)
			s = word_1.wup_similarity(word_2);
		#endtry
		if s > 0.8:
			word = word.replace("_", " ");
			_file.write(word + ",");
	#endfor
	_file.write("\n");
#endfor
