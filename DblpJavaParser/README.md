# `DblpJavaParser`

Java parser codes for the DBLP dataset, based on Java codes of [`DblpExampleParser`][dblp-example-parser].

[dblp-example-parser]: https://dblp.org/faq/1474681.html

## Requirements

- macOS, linux, or Windows Subsystem for Linux (WSL, not for sure)
- OpenJDK 8+
- Visual Studio Code

## Getting started

1. Download the DBLP dataset into `data` directory.
   - [`dblp-2022-05-02.xml.gz`](https://dblp.org/xml/release/dblp-2022-05-02.xml.gz)
   - [`dblp-2019-11-22.dtd`](https://dblp.org/xml/release/dblp-2019-11-22.dtd)
2. Decompress the XML file (`dblp-2022-05-02.xml.gz` => `dblp-2022-05-02.xml`)
   - `gzip --decompress dblp-2022-05-02.xml.gz`
3. Open this directory with Visual Studio Code.
4. Install ["Extension Pack for Java"][java-extension].
5. Open "Run and Debug" tab on the sidebar and run "Launch App" to run our parser codes on the downloaded dataset.

[java-extension]: https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack

## File structures

- `.vscode`: configurations for VS code, including `launch.json` to download
  data and run codes.
- `bin`: Directory where compiled Java class files will be stored. It won't
  be tracked by git.
- `data`: Directory to save the DBLP dataset. It should be downloaded manually.
- `src`: Java sources for parser codes. It includes largely two groups of codes:
  - `edu.snu.bkms`: our parser codes to run.
  - `org.dblp`: official library for data structures of the DBLP dataset
