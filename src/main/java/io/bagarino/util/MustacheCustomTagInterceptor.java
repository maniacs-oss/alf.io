/**
 * This file is part of bagarino.
 *
 * bagarino is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * bagarino is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with bagarino.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.bagarino.util;

import static org.apache.commons.lang3.StringUtils.substring;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.samskivert.mustache.Mustache;

/**
 * For formatting date in a mustache template.
 * 
 * <p>
 * Syntax is: {{#format-date}}{{yourDate}} FORMAT locale:YOUR_LOCALE{{/format-date}}.
 * </p>
 * <p>
 * Where
 * </p>
 * <ul>
 * <li>yourDate has been formatted following the java.time.ZonedDateTime
 * <li>FORMAT is a format understood by {@link DateTimeFormatter}</li>
 * <li>optional: locale:YOUR_LOCALE you can define the locale</li>
 * </ul>
 */
public class MustacheCustomTagInterceptor extends HandlerInterceptorAdapter {

	private static final String LOCALE_LABEL = "locale:";

	public static final Mustache.Lambda FORMAT_DATE = (frag, out) -> {
		String execution = frag.execute().trim();
		ZonedDateTime d = ZonedDateTime.parse(substring(execution, 0, execution.indexOf(" ")));
		Pair<String, Optional<Locale>> p = parseParams(execution);
		if (p.getRight().isPresent()) {
			out.write(DateTimeFormatter.ofPattern(p.getLeft(), p.getRight().get()).format(d));
		} else {
			out.write(DateTimeFormatter.ofPattern(p.getLeft()).format(d));
		}
	};

	private static Pair<String, Optional<Locale>> parseParams(String r) {

		int indexLocale = r.indexOf(LOCALE_LABEL), end = Math.min(r.length(),
				indexLocale != -1 ? indexLocale : r.length());
		String format = substring(r, r.indexOf(" "), end);

		//
		String[] res = r.split("\\s+");
		Optional<Locale> locale = Arrays.asList(res).stream().filter((s) -> s.startsWith(LOCALE_LABEL)).findFirst()
				.map((l) -> {
					return Locale.forLanguageTag(substring(l, LOCALE_LABEL.length()));
				});
		//

		return Pair.of(format, locale);
	}

	private static final Pattern ARG_PATTERN = Pattern.compile("\\[(.*?)\\]");

	private static Function<ModelAndView, Mustache.Lambda> HAS_ERROR = (mv) -> {
		return (frag, out) -> {
			Errors err = (Errors) mv.getModelMap().get("error");
			String execution = frag.execute().trim();
			Matcher matcher = ARG_PATTERN.matcher(execution);
			if (matcher.find()) {
				String field = matcher.group(1);
				if (err != null && err.hasFieldErrors(field)) {
					out.write(execution.substring(matcher.end(1)));
				}
			}
		};
	};

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

		if (modelAndView != null) {
			modelAndView.addObject("format-date", FORMAT_DATE);
			modelAndView.addObject("field-has-error", HAS_ERROR.apply(modelAndView));
		}

		super.postHandle(request, response, handler, modelAndView);
	}
}