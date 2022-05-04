TARGET=DblpJavaParser
PATH_DATA=../data

FILE_XML=$(PATH_DATA)/dblp-2022-05-02.xml
FILE_DTD=$(PATH_DATA)/dblp-2019-11-22.dtd


$(TARGET).class:
	javac -cp mmdb-2019-04-29.jar $(TARGET).java

run: DblpJavaParser.class
	java -Xmx8G -cp mmdb-2019-04-29.jar:. $(TARGET) $(FILE_XML) $(FILE_DTD)

clean:
	rm *.class

