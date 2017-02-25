/*
* Copyright 2017 John Ahlroos
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fi.jasoft.plugin;

import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.context.FileContext;
import io.bit3.jsass.importer.Import;
import io.bit3.jsass.importer.Importer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

public class LibSassCompiler {

    // Usage: 'LibSassCompiler [scss] [css] [unpackedThemes]
    public static void main(String[] args) throws Exception {
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }

        File unpackedThemes = new File(args[2]);
        File unpackedInputFile = Paths.get(
                unpackedThemes.getCanonicalPath(),
                inputFile.getParentFile().getName(),
                inputFile.getName()).toFile();

        Compiler compiler = new Compiler();
        Options options = new Options();

        Output output = compiler.compileFile(unpackedInputFile.toURI(), outputFile.toURI(), options);
        FileUtils.write(outputFile, output.getCss(), StandardCharsets.UTF_8.name());
    }
}