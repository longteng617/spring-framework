/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Helper class for use in bean factory implementations,
 * resolving values contained in bean definition objects
 * into the actual values applied to the target bean instance.
 *
 * <p>Operates on an {@link AbstractBeanFactory} and a plain
 * {@link org.springframework.beans.factory.config.BeanDefinition} object.
 * Used by {@link AbstractAutowireCapableBeanFactory}.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see AbstractAutowireCapableBeanFactory
 */
class BeanDefinitionValueResolver {

	private final AbstractAutowireCapableBeanFactory beanFactory;

	private final String beanName;

	private final BeanDefinition beanDefinition;

	private final TypeConverter typeConverter;


	/**
	 * Create a BeanDefinitionValueResolver for the given BeanFactory and BeanDefinition.
	 * @param beanFactory the BeanFactory to resolve against
	 * @param beanName the name of the bean that we work on
	 * @param beanDefinition the BeanDefinition of the bean that we work on
	 * @param typeConverter the TypeConverter to use for resolving TypedStringValues
	 */
	public BeanDefinitionValueResolver(AbstractAutowireCapableBeanFactory beanFactory, String beanName,
			BeanDefinition beanDefinition, TypeConverter typeConverter) {

		this.beanFactory = beanFactory;
		this.beanName = beanName;
		this.beanDefinition = beanDefinition;
		this.typeConverter = typeConverter;
	}


	/**
	 * Given a PropertyValue, return a value, resolving any references to other
	 * beans in the factory if necessary. The value could be:
	 * <li>A BeanDefinition, which leads to the creation of a corresponding
	 * new bean instance. Singleton flags and names of such "inner beans"
	 * are always ignored: Inner beans are anonymous prototypes.
	 * <li>A RuntimeBeanReference, which must be resolved.
	 * <li>A ManagedList. This is a special collection that may contain
	 * RuntimeBeanReferences or Collections that will need to be resolved.
	 * <li>A ManagedSet. May also contain RuntimeBeanReferences or
	 * Collections that will need to be resolved.
	 * <li>A ManagedMap. In this case the value may be a RuntimeBeanReference
	 * or Collection that will need to be resolved.
	 * <li>An ordinary object or {@code null}, in which case it's left alone.
	 * @param argName the name of the argument that the value is defined for
	 * @param value the value object to resolve
	 * @return the resolved object
	 */
	@Nullable
	public Object resolveValueIfNecessary(Object argName, @Nullable Object value) {
		// We must check each value to see whether it requires a runtime reference
		// to another bean to be resolved.
		// 我们必须检查每个值. 以查看他是否需要对另一个bean 的运行时引用才能解决
		// RuntimeBeanReference：当属性值对象是工厂中另一个bean的引用时. 使用不可变的占位符类. 在运行时解析

		// 如果 values 是 RuntimeBeanReference 实例
		if (value instanceof RuntimeBeanReference) {
			// 将 value 强转成 RuntimeBeanReference 对象
			RuntimeBeanReference ref = (RuntimeBeanReference) value;
			// 解析出对应 ref 所包装的 Bean 元信息(即 Bean 名,Bean 类型)的Bean 对象
			return resolveReference(argName, ref);
		}
		// RuntimeBeanNameReference 对应于 <idref bean = ""/>
		// idref 注入的是目标 bean 的id 而不是目标 bean 的实例. 同时使用 idref 容器在部署的时候还会验证这个名称的bean
		// 是否真实存在。其实idref 就是 value 一样. 只是将某个字符串注入到属性或者构造函数中。只不过注入的是某个Bean 定义的id属性值：
		// 即：<idref bean = "long" /> 等同于 <value>long</value>
		// 如果 values 是 RuntimeBeanNameReference 实例
		else if (value instanceof RuntimeBeanNameReference) {
			// 从 value 中获取引用的 bean 名
			String refName = ((RuntimeBeanNameReference) value).getBeanName();
			// 对 refName 进行解析。然后重新赋值给 refName
			refName = String.valueOf(doEvaluate(refName));
			// 如果该 bean 工厂不包含具有refName 的 beanDefinition 或 外部注册的 singleton 实例
			if (!this.beanFactory.containsBean(refName)) {
				// 抛出 BeanDefinition 存储异常：argName 的 Bean 引用中的 Bean 名'refName' 无效
				throw new BeanDefinitionStoreException(
						"Invalid bean name '" + refName + "' in bean reference for " + argName);
			}
			// 返回经过解析且经过检查其是否存在于Bean工厂的引用Bean名【refName】
			return refName;
		}
		// BeanDefinitionHolder:具有名称和别名的bean定义持有者。可以注册为内部bean的占位符
		// 如果 value 是 BeanDefinitionHolder 实例
		else if (value instanceof BeanDefinitionHolder) {
			// Resolve BeanDefinitionHolder: contains BeanDefinition with name and aliases.
			// 解决 BeanDefinitionHolder：包含具有名称和别名的 BeanDefinition
			// 将 value 强转为 BeanDefinitionHolder 对象
			BeanDefinitionHolder bdHolder = (BeanDefinitionHolder) value;
			// 根据 bdHolder 所封装的Bean名和 BeanDefinition 对象解析出内部Bean 对象
			return resolveInnerBean(argName, bdHolder.getBeanName(), bdHolder.getBeanDefinition());
		}
		// 一般在内部匿名bean 的配置才会出现 BeanDefinition
		// 如果 value 是 BeanDefinition 实例
		else if (value instanceof BeanDefinition) {
			// Resolve plain BeanDefinition, without contained name: use dummy name.
			// 解析出 BeanDefinition,不包含名称：使用虚拟名称
			// 将 value 强转为 BeanDefinition 对象
			BeanDefinition bd = (BeanDefinition) value;
			// 拼装内部Bean 名："(inner bean)#" + bd 的身份哈希码的十六进制字符串形式
			String innerBeanName = "(inner bean)" + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR +
					ObjectUtils.getIdentityHexString(bd);
			// 根据 innerBeanName 和 bd 解析出内部 Bean 对象
			return resolveInnerBean(argName, innerBeanName, bd);
		}
		// 如果 values 是 DependencyDescriptor 实例
		else if (value instanceof DependencyDescriptor) {
			// 定义一个用于存放所找到的所有候选Bean名的集合，初始化长度为 4
			Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
			// 根据 descriptor 的依赖类型解析出与 descriptor 所包装的对象匹配的候选Bean实例
			Object result = this.beanFactory.resolveDependency(
					(DependencyDescriptor) value, this.beanName, autowiredBeanNames, this.typeConverter);
			// 遍历 autowiredBeanNames
			for (String autowiredBeanName : autowiredBeanNames) {
				// 如果该Bean 工厂包含具有autowiredBeanName 的 beanDefinition 或 外部注册的singleton实例
				if (this.beanFactory.containsBean(autowiredBeanName)) {
					// 注册 autowiredBeanName 与 beanName 的依赖关系
					this.beanFactory.registerDependentBean(autowiredBeanName, this.beanName);
				}
			}
			// 返回与 descriptor 所包装的对象匹配的候选Bean 对象【result】
			return result;
		}
		else if (value instanceof ManagedArray) {
			// May need to resolve contained runtime references.
			ManagedArray array = (ManagedArray) value;
			Class<?> elementType = array.resolvedElementType;
			if (elementType == null) {
				String elementTypeName = array.getElementTypeName();
				if (StringUtils.hasText(elementTypeName)) {
					try {
						elementType = ClassUtils.forName(elementTypeName, this.beanFactory.getBeanClassLoader());
						array.resolvedElementType = elementType;
					}
					catch (Throwable ex) {
						// Improve the message by showing the context.
						throw new BeanCreationException(
								this.beanDefinition.getResourceDescription(), this.beanName,
								"Error resolving array type for " + argName, ex);
					}
				}
				else {
					// 让 elementType 默认使用 Object 类对象
					elementType = Object.class;
				}
			}
			// 解析 ManagedArray 对象，以得到解析后的数组对象
			return resolveManagedArray(argName, (List<?>) value, elementType);
		}
		// 对 ManagedList 进行解析
		else if (value instanceof ManagedList) {
			// May need to resolve contained runtime references.
			// 可能需要解析包含的运行时引用，解析 ManagedList 对象 以得到解析后的 List 对象并结果返回出去
			return resolveManagedList(argName, (List<?>) value);
		}
		// 对 ManagedSet 进行解析
		else if (value instanceof ManagedSet) {
			// May need to resolve contained runtime references.
			// 可能需要解析包含的运行时引用。解析 ManagedSet 对象，以得到解析后的 Set 对象并结果返回出去
			return resolveManagedSet(argName, (Set<?>) value);
		}
		// 对 ManagedMap 进行解析
		else if (value instanceof ManagedMap) {
			// May need to resolve contained runtime references.
			// 可能需要解析包含的运行时引用。解析 ManagedMap 对象，以得到解析后的Map对象并结果返回出去
			return resolveManagedMap(argName, (Map<?, ?>) value);
		}
		// 对 ManagedProperties 进行解析
		else if (value instanceof ManagedProperties) {
			// 将 value 强转为 Properties 对象
			Properties original = (Properties) value;
			// 定义一个用于存储将 original 的所有 Property 的键/值解析后的键/值的Properties 对象
			Properties copy = new Properties();
			// 遍历 original 键名为 propKey 值为 propValue
			original.forEach((propKey, propValue) -> {
				// 如果 propKey 是 TypedStringValue 类型
				if (propKey instanceof TypedStringValue) {
					// 在 propKey 封装的value 可解析成表达式的情况下，将propKey 封装的 value 评估为表达式并解析出表达式的值
					propKey = evaluate((TypedStringValue) propKey);
				}
				// 如果 propValue 是 TypedStringValue 类型
				if (propValue instanceof TypedStringValue) {
					propValue = evaluate((TypedStringValue) propValue);
				}
				if (propKey == null || propValue == null) {
					throw new BeanCreationException(
							this.beanDefinition.getResourceDescription(), this.beanName,
							"Error converting Properties key/value pair for " + argName + ": resolved to null");
				}
				// 将 propKey 和 propValue 添加到 copy 中
				copy.put(propKey, propValue);
			});
			return copy;
		}
		// 对 TypedStringValue 进行解析
		else if (value instanceof TypedStringValue) {
			// Convert value to target type here.
			// 在此处将 value 转换为目标类型,将 value 强转为 TypedStringValue 对象
			TypedStringValue typedStringValue = (TypedStringValue) value;
			// 在 typedStringValue 封装的 value 可解析成表达式的情况下，将 typedStringValue 封装的 value 苹果为表达式并解析出表达式的值
			Object valueObject = evaluate(typedStringValue);
			try {
				// 在 typeStringValue 中解析目标类型
				Class<?> resolvedTargetType = resolveTargetType(typedStringValue);
				// 如果 resolvedTargetType 不为null
				if (resolvedTargetType != null) {
					// 使用 typeConverter 将值转换为所需要的类型
					return this.typeConverter.convertIfNecessary(valueObject, resolvedTargetType);
				}
				else {
					// 返回并解析出来的表达式的值
					return valueObject;
				}
			}
			catch (Throwable ex) {
				// Improve the message by showing the context.
				throw new BeanCreationException(
						this.beanDefinition.getResourceDescription(), this.beanName,
						"Error converting typed String value for " + argName, ex);
			}
		}
		else if (value instanceof NullBean) {
			return null;
		}
		else {
			return evaluate(value);
		}
	}

	/**
	 * Evaluate the given value as an expression, if necessary.
	 * @param value the candidate value (may be an expression)
	 * @return the resolved value
	 */
	@Nullable
	protected Object evaluate(TypedStringValue value) {
		// 如果必要(value 可解析成表达式的情况下) 将 value 封装的 value 评估为表达式并解析出表达式的值
		Object result = doEvaluate(value.getValue());
		// 如果 result 与 value 所封装的 value 不相等
		if (!ObjectUtils.nullSafeEquals(result, value.getValue())) {
			// 将 value 标记为动态，即包含一个表达式，因此不进行缓存
			value.setDynamic();
		}
		// 返回 result
		return result;
	}

	/**
	 * Evaluate the given value as an expression, if necessary.
	 * @param value the original value (may be an expression)
	 * @return the resolved value if necessary, or the original value
	 */
	@Nullable
	protected Object evaluate(@Nullable Object value) {
		if (value instanceof String) {
			return doEvaluate((String) value);
		}
		else if (value instanceof String[]) {
			String[] values = (String[]) value;
			boolean actuallyResolved = false;
			Object[] resolvedValues = new Object[values.length];
			for (int i = 0; i < values.length; i++) {
				String originalValue = values[i];
				Object resolvedValue = doEvaluate(originalValue);
				if (resolvedValue != originalValue) {
					actuallyResolved = true;
				}
				resolvedValues[i] = resolvedValue;
			}
			return (actuallyResolved ? resolvedValues : values);
		}
		else {
			return value;
		}
	}

	/**
	 * Evaluate the given String value as an expression, if necessary.
	 * @param value the original value (may be an expression)
	 * @return the resolved value if necessary, or the original String value
	 */
	@Nullable
	private Object doEvaluate(@Nullable String value) {
		// 评估value 如果 value 是可解析表达式 会对其进行解析 否则直接返回 value
		return this.beanFactory.evaluateBeanDefinitionString(value, this.beanDefinition);
	}

	/**
	 * Resolve the target type in the given TypedStringValue.
	 * @param value the TypedStringValue to resolve
	 * @return the resolved target type (or {@code null} if none specified)
	 * @throws ClassNotFoundException if the specified type cannot be resolved
	 * @see TypedStringValue#resolveTargetType
	 */
	@Nullable
	protected Class<?> resolveTargetType(TypedStringValue value) throws ClassNotFoundException {
		if (value.hasTargetType()) {
			return value.getTargetType();
		}
		return value.resolveTargetType(this.beanFactory.getBeanClassLoader());
	}

	/**
	 * Resolve a reference to another bean in the factory.
	 */
	@Nullable
	private Object resolveReference(Object argName, RuntimeBeanReference ref) {
		try {
			// 定义用于一个存储Bean 对象的变量
			Object bean;
			// 获取另一个Bean 引用的 Bean 类型
			Class<?> beanType = ref.getBeanType();
			// 如果引用来自父工厂
			if (ref.isToParent()) {
				//获取父工厂
				BeanFactory parent = this.beanFactory.getParentBeanFactory();
				// 如果没有父工厂
				if (parent == null) {
					throw new BeanCreationException(
							this.beanDefinition.getResourceDescription(), this.beanName,
							"Cannot resolve reference to bean " + ref +
									" in parent factory: no parent factory available");
				}
				// 如果引用的 Bean 类型不为 null
				if (beanType != null) {
					// 从父工厂中获取引用的 bean 类型对应的 Bean 对象
					bean = parent.getBean(beanType);
				}
				else {
					// 否则，使用引用的 Bean 名 从父工厂中获取对应的 Bean 对象
					bean = parent.getBean(String.valueOf(doEvaluate(ref.getBeanName())));
				}
			}
			else {
				// 定义一个用于存储解析出来的 Bean 名的变量
				String resolvedName;
				// 如果 BeanType 不为null
				if (beanType != null) {
					// 解析与 BeanType 唯一匹配的 bean 实例 包括其 bean名
					NamedBeanHolder<?> namedBean = this.beanFactory.resolveNamedBean(beanType);
					// 让 bean 引用 nameBean 所封装的 Bean 对象
					bean = namedBean.getBeanInstance();
					// 让 resolveName 引用 namedBean 所封装的 Bean 名
					resolvedName = namedBean.getBeanName();
				}
				else {
					// 让 resolvedName 引用 ref 所包装的 Bean名
					resolvedName = String.valueOf(doEvaluate(ref.getBeanName()));
					// 获取 resolvedName 的 Bean 对象
					bean = this.beanFactory.getBean(resolvedName);
				}
				// 注册 beanName 与 dependentBeanName 的依赖关系到 Bean 工厂
				this.beanFactory.registerDependentBean(resolvedName, this.beanName);
			}
			// 如果 bean 对象是 NullBean
			if (bean instanceof NullBean) {
				bean = null;
			}
			// 返回解析出来对应ref 所包装的 Bean 元信息(即 Bean 名 Bean 类型) 的 Bean 对象
			return bean;
		}
		catch (BeansException ex) {
			throw new BeanCreationException(
					this.beanDefinition.getResourceDescription(), this.beanName,
					"Cannot resolve reference to bean '" + ref.getBeanName() + "' while setting " + argName, ex);
		}
	}

	/**
	 * Resolve an inner bean definition.
	 * @param argName the name of the argument that the inner bean is defined for
	 * @param innerBeanName the name of the inner bean
	 * @param innerBd the bean definition for the inner bean
	 * @return the resolved inner bean instance
	 */
	@Nullable
	private Object resolveInnerBean(Object argName, String innerBeanName, BeanDefinition innerBd) {
		RootBeanDefinition mbd = null;
		try {
			mbd = this.beanFactory.getMergedBeanDefinition(innerBeanName, innerBd, this.beanDefinition);
			// Check given bean name whether it is unique. If not already unique,
			// add counter - increasing the counter until the name is unique.
			String actualInnerBeanName = innerBeanName;
			if (mbd.isSingleton()) {
				actualInnerBeanName = adaptInnerBeanName(innerBeanName);
			}
			this.beanFactory.registerContainedBean(actualInnerBeanName, this.beanName);
			// Guarantee initialization of beans that the inner bean depends on.
			String[] dependsOn = mbd.getDependsOn();
			if (dependsOn != null) {
				for (String dependsOnBean : dependsOn) {
					this.beanFactory.registerDependentBean(dependsOnBean, actualInnerBeanName);
					this.beanFactory.getBean(dependsOnBean);
				}
			}
			// Actually create the inner bean instance now...
			Object innerBean = this.beanFactory.createBean(actualInnerBeanName, mbd, null);
			if (innerBean instanceof FactoryBean) {
				boolean synthetic = mbd.isSynthetic();
				innerBean = this.beanFactory.getObjectFromFactoryBean(
						(FactoryBean<?>) innerBean, actualInnerBeanName, !synthetic);
			}
			if (innerBean instanceof NullBean) {
				innerBean = null;
			}
			return innerBean;
		}
		catch (BeansException ex) {
			throw new BeanCreationException(
					this.beanDefinition.getResourceDescription(), this.beanName,
					"Cannot create inner bean '" + innerBeanName + "' " +
					(mbd != null && mbd.getBeanClassName() != null ? "of type [" + mbd.getBeanClassName() + "] " : "") +
					"while setting " + argName, ex);
		}
	}

	/**
	 * Checks the given bean name whether it is unique. If not already unique,
	 * a counter is added, increasing the counter until the name is unique.
	 * @param innerBeanName the original name for the inner bean
	 * @return the adapted name for the inner bean
	 */
	private String adaptInnerBeanName(String innerBeanName) {
		String actualInnerBeanName = innerBeanName;
		int counter = 0;
		String prefix = innerBeanName + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;
		while (this.beanFactory.isBeanNameInUse(actualInnerBeanName)) {
			counter++;
			actualInnerBeanName = prefix + counter;
		}
		return actualInnerBeanName;
	}

	/**
	 * For each element in the managed array, resolve reference if necessary.
	 */
	private Object resolveManagedArray(Object argName, List<?> ml, Class<?> elementType) {
		Object resolved = Array.newInstance(elementType, ml.size());
		for (int i = 0; i < ml.size(); i++) {
			Array.set(resolved, i, resolveValueIfNecessary(new KeyedArgName(argName, i), ml.get(i)));
		}
		return resolved;
	}

	/**
	 * For each element in the managed list, resolve reference if necessary.
	 */
	private List<?> resolveManagedList(Object argName, List<?> ml) {
		List<Object> resolved = new ArrayList<>(ml.size());
		for (int i = 0; i < ml.size(); i++) {
			resolved.add(resolveValueIfNecessary(new KeyedArgName(argName, i), ml.get(i)));
		}
		return resolved;
	}

	/**
	 * For each element in the managed set, resolve reference if necessary.
	 */
	private Set<?> resolveManagedSet(Object argName, Set<?> ms) {
		Set<Object> resolved = new LinkedHashSet<>(ms.size());
		int i = 0;
		for (Object m : ms) {
			resolved.add(resolveValueIfNecessary(new KeyedArgName(argName, i), m));
			i++;
		}
		return resolved;
	}

	/**
	 * For each element in the managed map, resolve reference if necessary.
	 */
	private Map<?, ?> resolveManagedMap(Object argName, Map<?, ?> mm) {
		Map<Object, Object> resolved = new LinkedHashMap<>(mm.size());
		mm.forEach((key, value) -> {
			Object resolvedKey = resolveValueIfNecessary(argName, key);
			Object resolvedValue = resolveValueIfNecessary(new KeyedArgName(argName, key), value);
			resolved.put(resolvedKey, resolvedValue);
		});
		return resolved;
	}


	/**
	 * Holder class used for delayed toString building.
	 */
	private static class KeyedArgName {

		private final Object argName;

		private final Object key;

		public KeyedArgName(Object argName, Object key) {
			this.argName = argName;
			this.key = key;
		}

		@Override
		public String toString() {
			return this.argName + " with key " + BeanWrapper.PROPERTY_KEY_PREFIX +
					this.key + BeanWrapper.PROPERTY_KEY_SUFFIX;
		}
	}

}
