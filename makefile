all: build

build: buildDir
	javac -d build src/Client.java src/Main.java src/Board.java src/Mark.java src/Move.java src/Position.java src/Bot.java

launch: build
	java -cp build Main

launch_cli: build
	java -cp build Client

buildDir:
	mkdir -p build

clean:
	rm -r build
