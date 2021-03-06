/*
 * Copyright (c) [2015] - [2017] Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';

import {CodenvyApiConfig} from './api/codenvy-api-config';
import {UniqueTeamNameValidator} from './api/validator/unique-team-name-validator.directive';

export class CodenvyComponentsConfig {

  constructor(register) {
    new CodenvyApiConfig(register);

    register.directive('uniqueTeamName', UniqueTeamNameValidator);
  }
}
