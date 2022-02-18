/*
 * Copyright (C) Scott Cranton, Jakub Korab, and Christian Posta
 * https://github.com/CamelCookbook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camelcookbook.extend.predicate;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;

public class MyPredicateInlineRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:start")
            .filter(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    final String body = exchange.getIn().getBody(String.class);
                    return ((body != null) && body.contains("Boston"));
                }
            })
                .to("mock:boston");
    }
}
