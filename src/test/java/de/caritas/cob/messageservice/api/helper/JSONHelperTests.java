package de.caritas.cob.messageservice.api.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import java.util.Optional;
import org.jeasy.random.EasyRandom;
import org.junit.Test;

public class JSONHelperTests {

  @Test
  public void convertAliasMessageDTOToString_Should_returnConvertedString_When_aliasMessageDTOIsvalid() {
    AliasMessageDTO aliasMessageDTO = new EasyRandom().nextObject(AliasMessageDTO.class);

    Optional<String> result = JSONHelper.convertAliasMessageDTOToString(aliasMessageDTO);

    assertThat(result.isPresent(), is(true));
  }

  @Test
  public void convertStringToAliasMessageDTO_Should_returnOptionalEmpty_When_jsonStringCanNotBeConverted() {
    Optional<AliasMessageDTO> result = JSONHelper.convertStringToAliasMessageDTO("alias");

    assertThat(result.isPresent(), is(false));
  }

}
