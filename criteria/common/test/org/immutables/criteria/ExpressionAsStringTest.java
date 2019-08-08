/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria;

import org.immutables.criteria.expression.DebugExpressionVisitor;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.StringMatcher;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Tests that expression is built correctly by "serializing" it to string.
 * It is easier to debug expression when it is presented as human-readable tree
 */
public class ExpressionAsStringTest {

  @Test
  public void string() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    assertExpressional(crit.bestFriend.isPresent(), "call op=IS_PRESENT path=bestFriend");
    assertExpressional(crit.bestFriend.isAbsent(), "call op=IS_ABSENT path=bestFriend");

    assertExpressional(crit.bestFriend.value().hobby.is("ski"),
            "call op=EQUAL path=bestFriend.hobby constant=ski");
    assertExpressional(crit.fullName.in("n1", "n2"), "call op=IN path=fullName constant=[n1, n2]");

    assertExpressional(crit.fullName.is("John").or().fullName.is("Marry"),
            "call op=OR",
                    "  call op=EQUAL path=fullName constant=John",
                    "  call op=EQUAL path=fullName constant=Marry");

    assertExpressional(crit.bestFriend.value().hobby.is("ski"),
                    "call op=EQUAL path=bestFriend.hobby constant=ski");
  }

  @Test
  public void with() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    assertExpressional(crit.with(c -> c.fullName.is("John")),
            "call op=EQUAL path=fullName constant=John");
    assertExpressional(crit.with(c -> c.fullName.is("John").nickName.isPresent()),
            "call op=AND",
            "  call op=EQUAL path=fullName constant=John",
            "  call op=IS_PRESENT path=nickName");
    assertExpressional(crit.fullName.with(f -> f.is("John")),
            "call op=EQUAL path=fullName constant=John");

    assertExpressional(crit.bestFriend.value().with(f -> f.hobby.is("aaa")),
            "call op=EQUAL path=bestFriend.hobby constant=aaa");

    assertExpressional(crit.bestFriend.value().hobby.with(h -> h.is("a").is("b")),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=a",
            "  call op=EQUAL path=bestFriend.hobby constant=b");

    assertExpressional(crit.bestFriend.value().hobby.with(h -> h.is("a").is("b")),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=a",
            "  call op=EQUAL path=bestFriend.hobby constant=b");

    assertExpressional(crit.bestFriend.value().hobby.with(h -> h.is("a").is("b"))
                   .fullName.isEmpty(),
            "call op=AND",
            "  call op=AND",
            "    call op=EQUAL path=bestFriend.hobby constant=a",
            "    call op=EQUAL path=bestFriend.hobby constant=b",
            "  call op=EQUAL path=fullName constant=");

    assertExpressional(PersonCriteria.person
                    .fullName.with(StringMatcher::isEmpty)
                    .or()
                    .fullName.with(StringMatcher::notEmpty),
            "call op=OR",
            "  call op=EQUAL path=fullName constant=",
            "  call op=NOT_EQUAL path=fullName constant="
    );

    assertExpressional(PersonCriteria.person
                    .fullName.with(StringMatcher::isEmpty)
                    .fullName.with(StringMatcher::notEmpty),
            "call op=AND",
            "  call op=EQUAL path=fullName constant=",
            "  call op=NOT_EQUAL path=fullName constant="
    );

  }

  @Test
  public void not() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    assertExpressional(crit.fullName.not(n -> n.is("John")),
            "call op=NOT",
                    "  call op=EQUAL path=fullName constant=John");

    assertExpressional(crit.not(f -> f.fullName.is("John").bestFriend.isPresent()),
            "call op=NOT",
                    "  call op=AND",
                    "    call op=EQUAL path=fullName constant=John",
                    "    call op=IS_PRESENT path=bestFriend");
  }

  @Test
  public void and() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    PersonCriteria<PersonCriteria.Self> other = PersonCriteria.person
            .and(crit.age.atMost(1)).and(crit.age.atLeast(2));

    assertExpressional(other, "call op=AND",
            "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
            "  call op=GREATER_THAN_OR_EQUAL path=age constant=2");

    assertExpressional(other.and(crit.age.is(3)),
            "call op=AND",
                    "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
                    "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
                    "  call op=EQUAL path=age constant=3");

    assertExpressional(other.and(crit.age.is(3)),
            "call op=AND",
                    "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
                    "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
                    "  call op=EQUAL path=age constant=3");
  }

  @Test
  public void or() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    PersonCriteria<PersonCriteria.Self> other = PersonCriteria.person
            .or(crit.age.atMost(1)).or(crit.age.atLeast(2));

    assertExpressional(other, "call op=OR",
            "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
            "  call op=GREATER_THAN_OR_EQUAL path=age constant=2");

    assertExpressional(other.or(crit.age.is(3)),
            "call op=OR",
                    "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
                    "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
                    "  call op=EQUAL path=age constant=3");

    assertExpressional(crit.or(crit.age.atMost(1)).or(crit.age.atLeast(2))
                    .or(crit.age.is(3)),
            "call op=OR",
            "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
            "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
            "  call op=EQUAL path=age constant=3");
  }

  /**
   * AND / ORs combined like {@code (A and B) or (C and D)}
   */
  @Test
  public void andOrCombined() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    // A or (B and C)
    assertExpressional(crit.age.is(1).or()
                    .age.is(2)
                    .age.is(3),
            "call op=OR",
            "  call op=EQUAL path=age constant=1",
            "  call op=AND",
            "    call op=EQUAL path=age constant=2",
            "    call op=EQUAL path=age constant=3");

    // (A and B) or C
    assertExpressional(crit.age.is(1)
                    .age.is(2)
                    .or()
                    .age.is(3),
            "call op=OR",
            "  call op=AND",
            "    call op=EQUAL path=age constant=1",
            "    call op=EQUAL path=age constant=2",
            "  call op=EQUAL path=age constant=3");

    // A or (B and C and D)
    assertExpressional(crit.age.is(1).or()
                    .age.is(2)
                    .age.is(3)
                    .age.is(4),
            "call op=OR",
            "  call op=EQUAL path=age constant=1",
            "  call op=AND",
            "    call op=EQUAL path=age constant=2",
            "    call op=EQUAL path=age constant=3",
            "    call op=EQUAL path=age constant=4");

    // (A and B) or (C and D)
    assertExpressional(crit.age.is(1)
                    .age.is(2)
            .or()
                    .age.is(3)
                    .age.is(4),
            "call op=OR",
            "  call op=AND",
            "    call op=EQUAL path=age constant=1",
            "    call op=EQUAL path=age constant=2",
            "  call op=AND",
            "    call op=EQUAL path=age constant=3",
            "    call op=EQUAL path=age constant=4");

    // (A and B and C) or D
    assertExpressional(crit.age.is(1)
                    .age.is(2)
                    .age.is(3)
            .or()
                    .age.is(4),
            "call op=OR",
            "  call op=AND",
            "    call op=EQUAL path=age constant=1",
            "    call op=EQUAL path=age constant=2",
            "    call op=EQUAL path=age constant=3",
            "  call op=EQUAL path=age constant=4");

  }

  @Test
  public void next() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;
    assertExpressional(crit.bestFriend.value().hobby.is("ski"), "call op=EQUAL path=bestFriend.hobby constant=ski");

    assertExpressional(crit.bestFriend.value().hobby.is("ski")
                    .age.is(22),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=ski",
            "  call op=EQUAL path=age constant=22");

    assertExpressional(crit.address.value().zip.is("12345"),
            "call op=EQUAL path=address.zip constant=12345");
    assertExpressional(PersonCriteria.person
                    .address.value().zip.is("12345")
                    .or()
                    .bestFriend.value().hobby.is("ski"),
            "call op=OR",
            "  call op=EQUAL path=address.zip constant=12345",
            "  call op=EQUAL path=bestFriend.hobby constant=ski");
  }

  @Test
  public void inner() {
    assertExpressional(PersonCriteria.person.bestFriend.value().with(f -> f.hobby.is("hiking")),
            "call op=EQUAL path=bestFriend.hobby constant=hiking");

    assertExpressional(PersonCriteria.person.bestFriend.value().with(f -> f.not(v -> v.hobby.is("hiking"))),
            "call op=NOT",
            "  call op=EQUAL path=bestFriend.hobby constant=hiking");

    assertExpressional(PersonCriteria.person.bestFriend
                    .value().with(f -> f.hobby.is("hiking").hobby.is("ski")),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=hiking",
            "  call op=EQUAL path=bestFriend.hobby constant=ski");

    assertExpressional(PersonCriteria.person.bestFriend
                    .value().with(f -> f.hobby.is("hiking").or().hobby.is("ski")),
            "call op=OR",
            "  call op=EQUAL path=bestFriend.hobby constant=hiking",
            "  call op=EQUAL path=bestFriend.hobby constant=ski");

    assertExpressional(PersonCriteria.person.bestFriend
                    .value().with(f -> f.hobby.is("hiking").or().hobby.is("ski"))
                    .fullName.isEmpty(),
            "call op=AND",
            "  call op=OR",
            "    call op=EQUAL path=bestFriend.hobby constant=hiking",
            "    call op=EQUAL path=bestFriend.hobby constant=ski",
            "  call op=EQUAL path=fullName constant="
    );

  }


  private static void assertExpressional(Criterion<?> crit, String ... expectedLines) {
    final StringWriter out = new StringWriter();
    Query query = Criterias.toQuery(crit);
    query.filter().ifPresent(f -> f.accept(new DebugExpressionVisitor<>(new PrintWriter(out))));
    final String expected = Arrays.stream(expectedLines).collect(Collectors.joining(System.lineSeparator()));
    Assert.assertEquals(expected, out.toString().trim());
  }

}
