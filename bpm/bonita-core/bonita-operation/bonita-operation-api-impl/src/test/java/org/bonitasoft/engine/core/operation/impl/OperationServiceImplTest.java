/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.core.operation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.core.expression.control.api.ExpressionResolverService;
import org.bonitasoft.engine.core.expression.control.model.SExpressionContext;
import org.bonitasoft.engine.core.operation.LeftOperandHandler;
import org.bonitasoft.engine.core.operation.LeftOperandHandlerProvider;
import org.bonitasoft.engine.core.operation.OperationExecutorStrategy;
import org.bonitasoft.engine.core.operation.OperationExecutorStrategyProvider;
import org.bonitasoft.engine.core.operation.exception.SOperationExecutionException;
import org.bonitasoft.engine.core.operation.model.SLeftOperand;
import org.bonitasoft.engine.core.operation.model.SOperation;
import org.bonitasoft.engine.core.operation.model.SOperatorType;
import org.bonitasoft.engine.core.operation.model.impl.SLeftOperandImpl;
import org.bonitasoft.engine.core.operation.model.impl.SOperationImpl;
import org.bonitasoft.engine.expression.model.SExpression;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OperationServiceImplTest {

    public static final String TYPE_1 = "type1";
    public static final String TYPE_2 = "type2";
    public static final String TYPE_3 = "type3";
    public static final String TYPE_4 = "type4";

    public static final long LEFT_OPERAND_CONTAINER_ID = 500L;
    public static final String LEFT_OPERAND_CONTAINER_TYPE = "process";

    private final class MatchLeftOperandName extends BaseMatcher<SLeftOperand> {

        private final String name;

        public MatchLeftOperandName(final String name) {
            this.name = name;
        }

        @Override
        public boolean matches(final Object item) {
            return item instanceof SLeftOperand && name.equals(((SLeftOperand) item).getName());
        }

        @Override
        public void describeTo(final Description description) {
        }
    }

    private final class MatchOperationType extends BaseMatcher<SOperation> {

        private final SOperatorType type;

        public MatchOperationType(final SOperatorType type) {
            this.type = type;
        }

        @Override
        public boolean matches(final Object item) {
            return item instanceof SOperation && type.equals(((SOperation) item).getType());
        }

        @Override
        public void describeTo(final Description description) {
        }
    }

    @Mock
    private ExpressionResolverService expressionResolverService;

    @Mock
    private LeftOperandHandlerProvider leftOperandHandlerProvider;

    @Mock
    private LeftOperandHandler leftOperandHandler1;

    @Mock
    private LeftOperandHandler leftOperandHandler2;

    @Mock
    private LeftOperandHandler leftOperandHandler3;

    @Mock
    private LeftOperandHandler leftOperandHandler4;

    @Mock
    private TechnicalLoggerService logger;

    @Mock
    private OperationExecutorStrategyProvider operationExecutorStrategyProvider;

    @Mock
    private OperationExecutorStrategy assignmentOperationExecutorStrategy;

    @Mock
    private OperationExecutorStrategy xPathOperationExecutorStrategy;

    @Mock
    private OperationExecutorStrategy javaMethodOperationExecutorStrategy;

    @Mock
    private PersistRightOperandResolver persistRightOperandResolver;

    @Captor
    private ArgumentCaptor<List<SLeftOperand>> leftOperandCaptor1;
    @Captor
    private ArgumentCaptor<List<SLeftOperand>> leftOperandCaptor2;

    private OperationServiceImpl operationServiceImpl;

    private SLeftOperandImpl buildLeftOperand(final String type, final String dataName) {
        final SLeftOperandImpl leftOperand = new SLeftOperandImpl();
        leftOperand.setType(type);
        leftOperand.setName(dataName);
        return leftOperand;
    }

    private SOperationImpl buildOperation(final String type, final String dataName, final SOperatorType operatorType) {
        final SOperationImpl sOperationImpl = new SOperationImpl();
        sOperationImpl.setLeftOperand(buildLeftOperand(type, dataName));
        sOperationImpl.setType(operatorType);
        return sOperationImpl;
    }

    private SOperationImpl buildOperation(final String type, final String dataName, final SOperatorType operatorType, final SExpression rightOperand) {
        final SOperationImpl operation = buildOperation(type, dataName, operatorType);
        operation.setRightOperand(rightOperand);
        return operation;
    }

    @Before
    public void before() throws SOperationExecutionException {
        doReturn(TYPE_1).when(leftOperandHandler1).getType();
        doReturn(TYPE_2).when(leftOperandHandler2).getType();
        doReturn(TYPE_3).when(leftOperandHandler3).getType();
        doReturn(TYPE_4).when(leftOperandHandler4).getType();
        doReturn(SOperatorType.ASSIGNMENT.name()).when(assignmentOperationExecutorStrategy).getOperationType();
        doReturn(SOperatorType.XPATH_UPDATE_QUERY.name()).when(xPathOperationExecutorStrategy).getOperationType();
        doReturn(SOperatorType.JAVA_METHOD.name()).when(javaMethodOperationExecutorStrategy).getOperationType();
        doReturn(assignmentOperationExecutorStrategy).when(operationExecutorStrategyProvider).getOperationExecutorStrategy(
                argThat(new MatchOperationType(SOperatorType.ASSIGNMENT)));
        doReturn(xPathOperationExecutorStrategy).when(operationExecutorStrategyProvider).getOperationExecutorStrategy(
                argThat(new MatchOperationType(SOperatorType.XPATH_UPDATE_QUERY)));
        doReturn(javaMethodOperationExecutorStrategy).when(operationExecutorStrategyProvider).getOperationExecutorStrategy(
                argThat(new MatchOperationType(SOperatorType.JAVA_METHOD)));
        doReturn(Arrays.asList(leftOperandHandler1, leftOperandHandler2, leftOperandHandler3, leftOperandHandler4)).when(leftOperandHandlerProvider)
                .getLeftOperandHandlers();

        given(assignmentOperationExecutorStrategy.shouldPersistOnNullValue()).willReturn(true);
        given(xPathOperationExecutorStrategy.shouldPersistOnNullValue()).willReturn(true);
        given(javaMethodOperationExecutorStrategy.shouldPersistOnNullValue()).willReturn(true);
        operationServiceImpl = new OperationServiceImpl(operationExecutorStrategyProvider, leftOperandHandlerProvider, expressionResolverService,
                persistRightOperandResolver, logger);
    }

    @Test
    public void should_UpdateLeftOperands_call_leftOperandHandlers() throws Exception {
        // given
        final Map<String, Object> inputValues = new HashMap<>();
        inputValues.put("data1", "value1");
        inputValues.put("data2", "value2");
        final SExpressionContext expressionContext = new SExpressionContext(123l, "containerType", 987L, inputValues);
        final Map<SLeftOperand, LeftOperandUpdateStatus> updates = new HashMap<>();
        updates.put(buildLeftOperand(TYPE_1, "data1"), new LeftOperandUpdateStatus(SOperatorType.ASSIGNMENT));
        updates.put(buildLeftOperand(TYPE_2, "data2"), new LeftOperandUpdateStatus(SOperatorType.DELETION));

        // when
        operationServiceImpl.updateLeftOperands(updates, 123, "containerType", expressionContext);

        // then
        verify(leftOperandHandler1).update(argThat(new MatchLeftOperandName("data1")), anyMapOf(String.class, Object.class), eq("value1"), eq(123l),
                eq("containerType"));
        verify(leftOperandHandler2).delete(argThat(new MatchLeftOperandName("data2")), eq(123l), eq("containerType"));
    }

    @Test
    public void executeOperator_should_call_OperationStrategies() throws Exception {
        // given
        final SExpression sExpression = mock(SExpression.class);
        final SOperation op1 = buildOperation(TYPE_1, "data1", SOperatorType.ASSIGNMENT, sExpression);
        final SOperation op2 = buildOperation(TYPE_2, "data2", SOperatorType.XPATH_UPDATE_QUERY, sExpression);
        final SOperation op3 = buildOperation(TYPE_2, "data2", SOperatorType.JAVA_METHOD, sExpression);
        final Map<String, Object> inputValues = new HashMap<>();
        final SExpressionContext expressionContext = new SExpressionContext(123l, "containerType", 987L, inputValues);

        final List<SOperation> operations = Arrays.asList(op1, op2, op3);
        given(persistRightOperandResolver.shouldPersistByPosition(0, operations)).willReturn(true);
        given(persistRightOperandResolver.shouldPersistByPosition(1, operations)).willReturn(false);
        given(persistRightOperandResolver.shouldPersistByPosition(2, operations)).willReturn(true);
        given(persistRightOperandResolver.shouldPersistByValue(anyObject(), eq(true), eq(true))).willReturn(true);
        given(persistRightOperandResolver.shouldPersistByValue(anyObject(), eq(false), eq(true))).willReturn(false);
        given(expressionResolverService.evaluate(sExpression, expressionContext)).willReturn(1983L);

        doReturn("value1").when(assignmentOperationExecutorStrategy).computeNewValueForLeftOperand(op1, 1983L, expressionContext, true);
        doReturn("value2").when(xPathOperationExecutorStrategy).computeNewValueForLeftOperand(op2, null, expressionContext, false);
        doReturn("value3").when(javaMethodOperationExecutorStrategy).computeNewValueForLeftOperand(op3, 1983L, expressionContext, true);

        // when
        operationServiceImpl.executeOperators(operations, expressionContext);

        // then
        assertThat(expressionContext.getInputValues().get("data1")).isEqualTo("value1");
        assertThat(expressionContext.getInputValues().get("data2")).isEqualTo("value3");
    }

    @Test
    public void should_retrieveLeftOperandsAndPutItInExpressionContextIfNotIn_do_not_override_value_in_map() throws Exception {
        // given
        final SOperation op1 = buildOperation(TYPE_2, "data1", SOperatorType.XPATH_UPDATE_QUERY);
        final SExpressionContext expressionContext = new SExpressionContext(123l, "containerType", 987L, Collections.<String, Object> singletonMap("data1",
                "originalValue"));

        // when
        operationServiceImpl.retrieveLeftOperandsAndPutItInExpressionContextIfNotIn(Arrays.asList(op1), 123, "containerType", expressionContext);

        // then
        verify(leftOperandHandler2, times(1)).loadLeftOperandInContext(eq(Arrays.asList(op1.getLeftOperand())), any(SExpressionContext.class),
                anyMapOf(String.class, Object.class));
        assertThat(expressionContext.getInputValues().get("data1")).isEqualTo("originalValue");
    }

    @Test
    public void executeOperatorsShouldReturnASingleUpdateForTheSameDataOfTheSameOperator() throws Exception {
        // given
        final SOperation operation1 = buildOperation(TYPE_1, "data1", SOperatorType.JAVA_METHOD);
        final SOperation operation2 = buildOperation(TYPE_1, "data1", SOperatorType.JAVA_METHOD);
        final List<SOperation> operations = Arrays.asList(operation1, operation2);
        final SExpressionContext expressionContext = new SExpressionContext(123l, "containerType", 987L, Collections.<String, Object> singletonMap("data1",
                "givenValue"));

        // when
        final Map<SLeftOperand, LeftOperandUpdateStatus> updates = operationServiceImpl.executeOperators(operations, expressionContext);

        // then
        assertThat(updates).hasSize(1);
        final SLeftOperandImpl leftOperand = buildLeftOperand("type1", "data1");
        assertThat(updates.containsKey(leftOperand));
        assertThat(updates.get(leftOperand)).isEqualToComparingFieldByField(new LeftOperandUpdateStatus(SOperatorType.JAVA_METHOD));
    }

    @Test
    public void executeOperatorsShouldReturnATwoUpdatesForTheDifferentDataOfTheSameOperator() throws Exception {
        // given
        final List<SOperation> operations = new ArrayList<>();
        operations.add(buildOperation(TYPE_1, "data2", SOperatorType.JAVA_METHOD));
        operations.add(buildOperation(TYPE_1, "data1", SOperatorType.JAVA_METHOD));
        final SExpressionContext expressionContext = new SExpressionContext(123l, "containerType", 987L, Collections.<String, Object> singletonMap("data1",
                "givenValue"));
        final OperationServiceImpl spy = spy(operationServiceImpl);

        // when
        final Map<SLeftOperand, LeftOperandUpdateStatus> updates = spy.executeOperators(operations, expressionContext);

        // then
        assertThat(updates).hasSize(2);
        final SLeftOperandImpl data2Key = buildLeftOperand("type1", "data2");
        final SLeftOperandImpl data1Key = buildLeftOperand("type1", "data1");
        assertThat(updates).containsKeys(data2Key, data1Key);
        assertThat(updates.get(data1Key)).isEqualToComparingFieldByField(new LeftOperandUpdateStatus(SOperatorType.JAVA_METHOD));
        assertThat(updates.get(data2Key)).isEqualToComparingFieldByField(new LeftOperandUpdateStatus(SOperatorType.JAVA_METHOD));
    }

    @Test
    public void executeOperationShouldDoBatchGet() throws SOperationExecutionException, SBonitaReadException {
        //given
        final List<SOperation> operations = new ArrayList<>();
        operations.add(buildOperation(TYPE_1, "data1", SOperatorType.JAVA_METHOD));
        operations.add(buildOperation(TYPE_1, "data2", SOperatorType.ASSIGNMENT));
        operations.add(buildOperation(TYPE_2, "data3", SOperatorType.ASSIGNMENT));
        operations.add(buildOperation(TYPE_2, "data4", SOperatorType.ASSIGNMENT));
        operations.add(buildOperation(TYPE_2, "data5", SOperatorType.ASSIGNMENT));
        final HashMap<String, Object> inputValues = new HashMap<>();
        final SExpressionContext expressionContext = new SExpressionContext(123l, "containerType", 987L, inputValues);

        //when
        operationServiceImpl.retrieveLeftOperandsAndPutItInExpressionContextIfNotIn(operations, 123l/* data container */, "containerType", expressionContext);

        //then
        verify(leftOperandHandler1, times(1)).loadLeftOperandInContext(leftOperandCaptor1.capture(), eq(expressionContext), eq(inputValues));
        verify(leftOperandHandler2, times(1)).loadLeftOperandInContext(leftOperandCaptor2.capture(), eq(expressionContext), eq(inputValues));
        final List<SLeftOperand> value1 = leftOperandCaptor1.getValue();
        final List<SLeftOperand> value2 = leftOperandCaptor2.getValue();
        assertThat(value1).containsOnly(operations.get(0).getLeftOperand(), operations.get(1).getLeftOperand());
        assertThat(value2).containsOnly(operations.get(2).getLeftOperand(), operations.get(3).getLeftOperand(), operations.get(4).getLeftOperand());
    }

    @Test
    public void executeOperationShouldNotGetWhenInAnOtherContainer() throws SOperationExecutionException, SBonitaReadException {
        //given
        final List<SOperation> operations = new ArrayList<>();
        operations.add(buildOperation(TYPE_1, "data1", SOperatorType.JAVA_METHOD));
        operations.add(buildOperation(TYPE_1, "data2", SOperatorType.ASSIGNMENT));
        final HashMap<String, Object> inputValues = new HashMap<>();
        final SExpressionContext expressionContext = new SExpressionContext(123l, "containerType", 987L, inputValues);

        //when
        operationServiceImpl.retrieveLeftOperandsAndPutItInExpressionContextIfNotIn(operations, 124l/* an other data container */, "containerType",
                expressionContext);

        //then
        verify(leftOperandHandler1, times(0)).loadLeftOperandInContext(anyListOf(SLeftOperand.class), any(SExpressionContext.class),
                anyMapOf(String.class, Object.class));
        verify(leftOperandHandler1, times(0)).loadLeftOperandInContext(any(SLeftOperand.class), any(SExpressionContext.class),
                anyMapOf(String.class, Object.class));
    }

    @Test
    public void should_not_update_left_operand_context_when_new_update() throws Exception {
        //given
        final Map<SLeftOperand, LeftOperandUpdateStatus> leftOperands = Collections.<SLeftOperand, LeftOperandUpdateStatus> singletonMap(
                buildLeftOperand("type1", "data1"), new LeftOperandUpdateStatus(SOperatorType.ASSIGNMENT));

        //when
        final boolean shouldUpdateLeftOperandContext = operationServiceImpl.shouldUpdateLeftOperandContext(leftOperands, buildLeftOperand("type1", "data1"),
                new LeftOperandUpdateStatus(
                        SOperatorType.JAVA_METHOD));

        //then
        assertThat(shouldUpdateLeftOperandContext).isFalse();
    }

    @Test
    public void should_not_update_left_operand_context_when_previous_was_a_deletion() throws Exception {
        //given
        final Map<SLeftOperand, LeftOperandUpdateStatus> leftOperands = Collections.<SLeftOperand, LeftOperandUpdateStatus> singletonMap(
                buildLeftOperand("type1", "data1"), new LeftOperandUpdateStatus(SOperatorType.DELETION));

        //when
        final boolean shouldUpdateLeftOperandContext = operationServiceImpl.shouldUpdateLeftOperandContext(leftOperands, buildLeftOperand("type1", "data1"),
                new LeftOperandUpdateStatus(
                        SOperatorType.JAVA_METHOD));

        //then
        assertThat(shouldUpdateLeftOperandContext).isFalse();
    }

    @Test
    public void should_update_left_operand_context_when_not_in_the_context() throws Exception {
        //given
        final Map<SLeftOperand, LeftOperandUpdateStatus> leftOperands = Collections.emptyMap();

        //when
        final boolean shouldUpdateLeftOperandContext = operationServiceImpl.shouldUpdateLeftOperandContext(leftOperands, buildLeftOperand("type1", "data1"),
                new LeftOperandUpdateStatus(
                        SOperatorType.JAVA_METHOD));

        //then
        assertThat(shouldUpdateLeftOperandContext).isTrue();
    }

    @Test
    public void should_update_left_operand_context_when_new_operation_is_deletion() throws Exception {
        //given
        final LeftOperandUpdateStatus previousUpdateState = new LeftOperandUpdateStatus(SOperatorType.JAVA_METHOD);
        final Map<SLeftOperand, LeftOperandUpdateStatus> leftOperands = Collections.<SLeftOperand, LeftOperandUpdateStatus> singletonMap(
                buildLeftOperand("type1", "data1"), previousUpdateState);

        //when new java method must be persisted again
        final boolean shouldUpdateLeftOperandContext = operationServiceImpl.shouldUpdateLeftOperandContext(leftOperands, buildLeftOperand("type1", "data1"),
                new LeftOperandUpdateStatus(
                        SOperatorType.DELETION));

        //then
        assertThat(shouldUpdateLeftOperandContext).isTrue();
    }

}
