javac Suggest.java
java Suggest alice_in_wonderland.txt 729 #first test case
java Suggest alice_in_wonderland.txt 9 #single character  in 'wxyz'
java Suggest alice_in_wonderland.txt 99 #duplicate but no match in corpus
java Suggest alice_in_wonderland.txt 22837745527 #long word 'Caterpillar'
java Suggest alice_in_wonderland.txt -143 #invalid into sequence
