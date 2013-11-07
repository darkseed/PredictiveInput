Question:
Design a predictive text input system (as used by
cellphones using a numeric keypad). Your program should take a
training dataset file (the text of Alice in Wonderland in this case)
and a numeric input, e.g. "227", and then output the set of words
corresponding to the input, e.g. "car", "cap", "bar", in order of word
popularity in the corpus. No additional language
dictionaries/datasets should be used.

Answer:
Assumptions - the txt file is the only source of corpus, it's all in English words. For negative expressions including [didn't, ain't] they are not included in the dictionary since they are considered variants.

Design - For the exact match and prefix matches, I have created two separate data structure for the two problems. when reading the data from the file, it will build the exact match map based on the numeric sequence itself, which each Map.Entry has a collection of words, with their count in the corpus for sorting. The prefix match is an implementation of prefix tree(trie) data structure which provides the path information while traversing the tree. I think it is a good data structure for the prefix matching problem.