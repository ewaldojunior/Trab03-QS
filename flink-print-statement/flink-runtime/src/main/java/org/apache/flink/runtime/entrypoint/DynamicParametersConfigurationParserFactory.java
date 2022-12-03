/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.entrypoint;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ConfigurationUtils;
import org.apache.flink.runtime.entrypoint.parser.ParserResultFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import javax.annotation.Nonnull;

import java.util.Properties;

import static org.apache.flink.runtime.entrypoint.parser.CommandLineOptions.DYNAMIC_PROPERTY_OPTION;

/**
 * {@code DynamicParametersConfigurationParserFactory} can be used to extract the dynamic parameters
 * from command line.
 */
public class DynamicParametersConfigurationParserFactory
        implements ParserResultFactory<Configuration> {

    public static Options options() {
        final Options options = new Options();
        options.addOption(DYNAMIC_PROPERTY_OPTION);

        return options;
    }

    @Override
    public Options getOptions() {
        return options();
    }

    @Override
    public Configuration createResult(@Nonnull CommandLine commandLine) {
        final Properties dynamicProperties =
                commandLine.getOptionProperties(DYNAMIC_PROPERTY_OPTION.getOpt());
        return ConfigurationUtils.createConfiguration(dynamicProperties);
    }
}
