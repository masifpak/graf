package fi.free.onefree.grupi.batch.filestager.mapper;

import org.springframework.batch.item.file.mapping.DefaultLineMapper;

import fi.free.flj.util.StringUtil;
import fi.free.onefree.grupi.entity.StgGrupiItem;

/**
 * Custom mapper for StgGrupiFile line.
 */
public class StgGrupiFileLineMapper extends DefaultLineMapper<StgGrupiItem> {

	@Override
	public StgGrupiItem mapLine(String line, int lineNumber) throws Exception {
		// Skip empty csv lines
		if (StringUtil.isNullOrBlank(line)) {
			return null;
		}

		var stgGrupiItem = super.mapLine(line, lineNumber);
		stgGrupiItem.setRowNum(lineNumber);

		return stgGrupiItem;
	}

}