package com.un.dto;

import java.util.List;

import lombok.Data;

@Data
public class RoomAndInvigilatorDetail {
	String id;
	List<InvesilotersDTO> invesiloters;
}
