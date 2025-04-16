/*
 * Copyright © 2017-2019 Cask Data, Inc.
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

package io.cdap.directives.column;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.cdap.cdap.etl.api.Lookup;
import io.cdap.cdap.etl.api.StageMetrics;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.parser.Token;
import io.cdap.wrangler.api.parser.TokenType;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test class for AggregateStats directive.
 */
public class AggregateStatsDirectiveTest {

  /**
   * Mock Token implementation for testing.
   */
  private static class MockToken implements Token {
    private final String value;

    /**
     * Constructor for MockToken.
     *
     * @param value the token value
     */
    MockToken(String value) {
      this.value = value;
    }

    @Override
    public String value() {
      return value;
    }

    @Override
    public TokenType type() {
      return TokenType.IDENTIFIER;
    }

    @Override
    public JsonElement toJson() {
      return new JsonPrimitive(value);
    }
  }

  /**
   * Mock Arguments implementation for testing.
   */
  private static class MockArguments implements Arguments {
    private final Map<String, Token> values = new HashMap<>();

    /**
     * Adds a key-value pair to the arguments.
     *
     * @param key   the argument key
     * @param value the argument value
     */
    public void put(String key, String value) {
      values.put(key, new MockToken(value));
    }

    @Override
    public <T extends Token> T value(String name) {
      return (T) values.get(name);
    }

    @Override
    public int size() {
      return values.size();
    }

    @Override
    public boolean contains(String name) {
      return values.containsKey(name);
    }

    @Override
    public TokenType type(String name) {
      return values.get(name).type();
    }

    @Override
    public int line() {
      return 1;
    }

    @Override
    public int column() {
      return 1;
    }

    @Override
    public String source() {
      return "mock";
    }

    @Override
    public JsonElement toJson() {
      return null;
    }
  }

  /**
   * Mock ExecutorContext implementation for testing.
   */
  private static class MockExecutorContext implements ExecutorContext {
    @Override
    public Environment getEnvironment() {
      return Environment.TESTING;
    }

    @Override
    public String getNamespace() {
      return "default";
    }

    @Override
    public StageMetrics getMetrics() {
      return null;
    }

    @Override
    public String getContextName() {
      return "mockContext";
    }

    @Override
    public Map<String, String> getProperties() {
      return Collections.emptyMap();
    }

    @Override
    public URL getService(String applicationId, String serviceId) {
      return null;
    }

    @Override
    public TransientStore getTransientStore() {
      return null;
    }

    @Override
    public <T> Lookup<T> provide(String s, Map<String, String> map) {
      return null;
    }
  }

  /**
   * Test for the AggregateStats directive with valid inputs.
   */
  @Test
  public void testAggregateStats() {
    AggregateStats directive = new AggregateStats();
    MockArguments arguments = new MockArguments();
    arguments.put("size-column", "size");
    arguments.put("duration-column", "duration");
    arguments.put("output-size-column", "totalSize");
    arguments.put("output-time-column", "totalDuration");

    directive.initialize(arguments);

    List<Row> rows = new ArrayList<>();
    rows.add(new Row("size", "1KB").add("duration", "1s"));
    rows.add(new Row("size", 2048L).add("duration", 1500L));

    List<Row> output = directive.execute(rows, new MockExecutorContext());
    Row result = output.get(0);

    Assert.assertEquals(3072L, result.getValue("totalSize"));
    Assert.assertEquals(2500L, result.getValue("totalDuration"));
  }

  /**
   * Test for the AggregateStats directive with invalid byte size input.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidByteSizeInput() {
    AggregateStats directive = new AggregateStats();
    MockArguments arguments = new MockArguments();
    arguments.put("size-column", "size");
    arguments.put("duration-column", "duration");
    arguments.put("output-size-column", "totalSize");
    arguments.put("output-time-column", "totalDuration");

    directive.initialize(arguments);

    Row row = new Row("size", "invalid").add("duration", "1s");
    directive.execute(Collections.singletonList(row), new MockExecutorContext());
  }

  /**
   * Test for the AggregateStats directive with invalid time input.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidTimeInput() {
    AggregateStats directive = new AggregateStats();
    MockArguments arguments = new MockArguments();
    arguments.put("size-column", "size");
    arguments.put("duration-column", "duration");
    arguments.put("output-size-column", "totalSize");
    arguments.put("output-time-column", "totalDuration");

    directive.initialize(arguments);

    Row row = new Row("size", "1KB").add("duration", "invalid");
    directive.execute(Collections.singletonList(row), new MockExecutorContext());
  }
}
