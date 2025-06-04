/****************************************************************************
 * Copyright (c) 2025 Composent, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
  * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.ai.a2a.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.ecf.ai.a2a.annotations.AgentAuthentication;
import org.eclipse.ecf.ai.a2a.annotations.AgentCapabilities;
import org.eclipse.ecf.ai.a2a.annotations.AgentCard;
import org.eclipse.ecf.ai.a2a.annotations.AgentProvider;
import org.eclipse.ecf.ai.a2a.annotations.AgentSkill;

public record AgentCardDescription(
		AgentCapabilitiesDescription capabilities, 
		List<String> defaultInputModes,
		List<String> defaultOutputModes, 
		String description, 
		String documentationUrl, 
		String iconUrl, 
		String name,
		AgentProviderDescription provider, 
		List<AgentSkillDescription> skills,
		boolean supportsAuthenticatedExtendedCard, 
		String url, 
		String version,
		AgentAuthenticationDescription authentication) {

	private static <T> List<T> a2l(@SuppressWarnings("unchecked") T... array) {
		return Arrays.asList(array);
	}

	public static AgentCardDescription fromService(Object svc, String serviceClass) {
		Optional<Class<?>> optClass = Arrays.asList(svc.getClass().getInterfaces()).stream().filter(c -> {
			return c.getName().equals(serviceClass);
		}).findFirst();
		return optClass.isPresent() ? AgentCardDescription.fromClass(optClass.get()) : null;
	}

	public static AgentCardDescription fromClass(Class<?> clazz) {
		AgentCard ac = clazz.getAnnotation(AgentCard.class);
		if (ac != null) {
			AgentProvider ap = ac.provider();
			AgentProviderDescription apd = ap != null ? new AgentProviderDescription(ap.organization(), ap.url())
					: null;
			AgentSkill[] skills = ac.skills();
			List<AgentSkillDescription> skillDescriptions = skills != null ? a2l(skills).stream().map(s -> {
				return new AgentSkillDescription(s.description(), a2l(s.examples()), s.id(), a2l(s.inputModes()),
						s.name(), a2l(s.outputModes()), a2l(s.tags()));
			}).filter(Objects::nonNull).collect(Collectors.toList()) : Collections.emptyList();

			AgentCapabilities acap = ac.capabilities();
			AgentCapabilitiesDescription acd = acap != null
					? new AgentCapabilitiesDescription(acap.pushNotifications(), acap.stateTransitionHistory(),
							acap.streaming())
					: null;

			AgentAuthentication aa = ac.authentication();
			AgentAuthenticationDescription aad = aa != null
					? new AgentAuthenticationDescription(a2l(aa.schemes()), aa.credentials())
					: null;

			return new AgentCardDescription(acd, a2l(ac.defaultInputModes()), a2l(ac.defaultOutputModes()),
					ac.description(), ac.documentationUrl(), ac.iconUrl(), ac.name(), apd, skillDescriptions,
					ac.supportsAuthenticatedExtendedCard(), ac.url(), ac.version(), aad);
		} else
			return null;
	}
}
