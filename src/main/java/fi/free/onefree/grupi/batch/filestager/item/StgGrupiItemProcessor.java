package fi.free.onefree.grupi.batch.filestager.item;

import org.springframework.batch.item.ItemProcessor;

import fi.free.onefree.grupi.entity.StgGrupiItem;
import fi.free.onefree.grupi.enums.StgGrupiItemStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Item processor for StgGrupiItems.
 */
@Slf4j
@RequiredArgsConstructor
public class StgGrupiItemProcessor implements ItemProcessor<StgGrupiItem, StgGrupiItem> {

	private final Long stgGrupiFileId;

	@Override
	public StgGrupiItem process(StgGrupiItem item) {
		log.trace("In process");

		item.setStgGrupiFileId(stgGrupiFileId);
		item.setStatus(StgGrupiItemStatusEnum.PENDING);

		return item;
	}

}