/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

 package io.cdap.wrangler.parser;

 import io.cdap.wrangler.api.CompileException;
 import io.cdap.wrangler.api.CompileStatus;
 import io.cdap.wrangler.api.Compiler;
 import io.cdap.wrangler.api.RecipeSymbol;
 import org.antlr.v4.runtime.CharStream;
 import org.antlr.v4.runtime.CharStreams;
 import org.antlr.v4.runtime.CommonTokenStream;
 import org.antlr.v4.runtime.tree.ParseTree;
 import org.antlr.v4.tool.GrammarParserInterpreter;
 import org.apache.twill.filesystem.Location;
 
 import java.io.InputStream;
 import java.nio.file.Path;
 
 /**
  * This class implements the {@link Compiler} interface for recipes.
  * It provides methods to compile directives from various sources like
  * strings, locations, and file paths.
  */
 public final class RecipeCompiler implements Compiler {
 
   /**
    * Compiles a recipe from a string.
    *
    * @param recipe The recipe string to be compiled
    * @return The compilation status
    * @throws CompileException If there is an error during compilation
    */
   @Override
   public CompileStatus compile(String recipe) throws CompileException {
     return compile(CharStreams.fromString(recipe));
   }
 
   /**
    * Compiles a recipe from a Location.
    *
    * @param location The location containing the recipe to be compiled
    * @return The compilation status
    * @throws CompileException If there is an error during compilation
    */
   @Override
   public CompileStatus compile(Location location) throws CompileException {
     try (InputStream is = location.getInputStream()) {
       return compile(CharStreams.fromStream(is));
     } catch (Exception e) {
       throw new CompileException(e.getMessage(), e);
     }
   }
 
   /**
    * Compiles a recipe from a Path.
    *
    * @param path The path to the file containing the recipe to be compiled
    * @return The compilation status
    * @throws CompileException If there is an error during compilation
    */
   @Override
   public CompileStatus compile(Path path) throws CompileException {
     try {
       return compile(CharStreams.fromPath(path));
     } catch (Exception e) {
       throw new CompileException(e.getMessage(), e);
     }
   }
 
   /**
    * Internal compile method that handles the actual compilation process.
    *
    * @param stream The character stream containing the recipe to be compiled
    * @return The compilation status
    * @throws CompileException If there is an error during compilation
    */
   private CompileStatus compile(CharStream stream) throws CompileException {
     try {
       SyntaxErrorListener errorListener = new SyntaxErrorListener();
       DirectivesLexer lexer = new DirectivesLexer(stream);
       lexer.removeErrorListeners();
       lexer.addErrorListener(errorListener);
 
       DirectivesParser parser = new DirectivesParser(new CommonTokenStream(lexer));
       parser.removeErrorListeners();
       parser.addErrorListener(errorListener);
       parser.setErrorHandler(new GrammarParserInterpreter.BailButConsumeErrorStrategy());
       parser.setBuildParseTree(true);
       ParseTree tree = parser.statements();
 
       if (errorListener.hasErrors()) {
         return new CompileStatus(true, errorListener.iterator());
       }
 
       RecipeVisitor visitor = new RecipeVisitor();
       visitor.visit(tree);
       RecipeSymbol symbol = visitor.getCompiledUnit();
       return new CompileStatus(symbol);
     } catch (StringIndexOutOfBoundsException e) {
       throw new CompileException("Issue in compiling directives");
     }
   }
 }
