package kr.co.bookpool.common.discord;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Discord 웹훅 메시지 페이로드.
 * @see <a href="https://discord.com/developers/docs/resources/webhook#execute-webhook">Execute Webhook</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiscordMessageRequest(
	String content,
	String username,
	@JsonProperty("avatar_url") String avatarUrl
) {

	public static DiscordMessageRequest of(String content, String username) {
		return new DiscordMessageRequest(content, username, null);
	}
}
