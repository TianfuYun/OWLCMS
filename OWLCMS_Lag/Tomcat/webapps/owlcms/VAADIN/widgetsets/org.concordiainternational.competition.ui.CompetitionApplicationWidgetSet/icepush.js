if (!window.ice) {
    window.ice = new Object;
}
if (!window.ice.icepush) {
    (function(namespace) {
        window.ice.icepush = true;
function apply(fun, arguments) {
    return fun.apply(fun, arguments);
}
function withArguments() {
    var args = arguments;
    return function(fun) {
        apply(fun, args);
    };
}
function let(definition) {
    return function() {
        return apply(definition, arguments);
    };
}
function curry() {
    var args = arguments;
    return function() {
        var curriedArguments = [];
        var fun = args[0];
        for (var i = 1; i < args.length; i++) curriedArguments.push(args[i]);
        for (var j = 0; j < arguments.length; j++) curriedArguments.push(arguments[j]);
        return apply(fun, curriedArguments);
    };
}
function $witch(tests, defaultRun) {
    return function(val) {
        var args = arguments;
        var conditions = [];
        var runs = [];
        tests(function(condition, run) {
            conditions.push(condition);
            runs.push(run);
        });
        var size = conditions.length;
        for (var i = 0; i < size; i++) {
            if (apply(conditions[i], args)) {
                return apply(runs[i], args);
            }
        }
        if (defaultRun) apply(defaultRun, args);
    };
}
function identity(arg) {
    return arg;
}
function negate(b) {
    return !b;
}
function greater(a, b) {
    return a > b;
}
function less(a, b) {
    return a < b;
}
function not(a) {
    return !a;
}
function multiply(a, b) {
    return a * b;
}
function plus(a, b) {
    return a + b;
}
function max(a, b) {
    return a > b ? a : b;
}
function increment(value, step) {
    return value + (step ? step : 1);
}
function decrement(value, step) {
    return value - (step ? step : 1);
}
function any() {
    return true;
}
function none() {
    return false;
}
function noop() {
}
function isArray(a) {
    return a && !!a.push;
}
function isString(s) {
    return typeof s == 'string';
}
function isNumber(s) {
    return typeof s == 'number';
}
function isIndexed(s) {
    return !!s.length;
}
function isObject(o) {
    return o.instanceTag == o;
}
var uid = (function() {
    var id = 0;
    return function() {
        return id++;
    };
})();
function operationNotSupported() {
    throw 'operation not supported';
}
function operator(defaultOperation) {
    return function() {
        var args = arguments;
        var instance = arguments[0];
        if (instance.instanceTag && instance.instanceTag == instance) {
            var method = instance(arguments.callee);
            if (method) {
                return method.apply(method, args);
            } else {
                operationNotSupported();
            }
        } else {
            return defaultOperation ? defaultOperation.apply(defaultOperation, args) : operationNotSupported();
        }
    };
}
var asString = operator(String);
var asNumber = operator(Number);
var hash = operator(function(o) {
    var s;
    if (isString(o)) {
        s = o;
    } else if (isNumber(o)) {
        return Math.abs(Math.round(o));
    } else {
        s = o.toString();
    }
    var h = 0;
    for (var i = 0, l = s.length; i < l; i++) {
        var c = parseInt(s[i], 36);
        if (!isNaN(c)) {
            h = c + (h << 6) + (h << 16) - h;
        }
    }
    return Math.abs(h);
});
var equal = operator(function(a, b) {
    return a == b;
});
function object(definition) {
    var operators = [];
    var methods = [];
    var unknown = null;
    var id = uid();
    operators.push(hash);
    methods.push(function(self) {
        return id;
    });
    operators.push(equal);
    methods.push(function(self, other) {
        return self == other;
    });
    operators.push(asString);
    methods.push(function(self) {
        return 'Object:' + id.toString(16);
    });
    definition(function(operator, method) {
        var size = operators.length;
        for (var i = 0; i < size; i++) {
            if (operators[i] == operator) {
                methods[i] = method;
                return;
            }
        }
        operators.push(operator);
        methods.push(method);
    }, function(method) {
        unknown = method;
    });
    function self(operator) {
        var size = operators.length;
        for (var i = 0; i < size; i++) {
            if (operators[i] == operator) {
                return methods[i];
            }
        }
        return unknown;
    }
    return self.instanceTag = self;
}
function objectWithAncestors() {
    var definition = arguments[0];
    var args = arguments;
    var o = object(definition);
    function self(operator) {
        var method = o(operator);
        if (method) {
            return method;
        } else {
            var size = args.length;
            for (var i = 1; i < size; i++) {
                var ancestor = args[i];
                var overriddenMethod = ancestor(operator);
                if (overriddenMethod) {
                    return overriddenMethod;
                }
            }
            return null;
        }
    }
    return self.instanceTag = self;
}
var indexOf = operator($witch(function(condition) {
    condition(isString, function(items, item) {
        return items.indexOf(item);
    });
    condition(isArray, function(items, item) {
        for (var i = 0, size = items.length; i < size; i++) {
            if (items[i] == item) {
                return i;
            }
        }
        return -1;
    });
    condition(any, operationNotSupported);
}));
var concatenate = operator(function(items, other) {
    return items.concat(other);
});
var append = operator($witch(function(condition) {
    condition(isArray, function(items, item) {
        items.push(item);
        return items;
    });
    condition(any, operationNotSupported);
}));
var insert = operator($witch(function(condition) {
    condition(isArray, function(items, item) {
        items.unshift(item);
        return items;
    });
    condition(any, operationNotSupported);
}));
var each = operator(function(items, iterator) {
    var size = items.length;
    for (var i = 0; i < size; i++) iterator(items[i], i);
});
var inject = operator(function(items, initialValue, injector) {
    var tally = initialValue;
    var size = items.length;
    for (var i = 0; i < size; i++) {
        tally = injector(tally, items[i]);
    }
    return tally;
});
var select = operator($witch(function(condition) {
    condition(isArray, function(items, selector) {
        return inject(items, [], function(tally, item) {
            return selector(item) ? append(tally, item) : tally;
        });
    });
    condition(isString, function(items, selector) {
        return inject(items, '', function(tally, item) {
            return selector(item) ? concatenate(tally, item) : tally;
        });
    });
    condition(isIndexed, function(items, selector) {
        return Stream(function(cellConstructor) {
            function selectingStream(start, end) {
                if (start > end) return null;
                var item = items[start];
                return selector(item) ?
                       function() {
                           return cellConstructor(item, selectingStream(start + 1, end));
                       } : selectingStream(start + 1, end);
            }
            return selectingStream(0, items.length - 1);
        });
    });
}));
var detect = operator(function(items, iterator, notDetectedThunk) {
    var size = items.length;
    for (var i = 0; i < size; i++) {
        var element = items[i];
        if (iterator(element, i)) {
            return element;
        }
    }
    return notDetectedThunk ? notDetectedThunk(items) : null;
});
var contains = operator($witch(function(condition) {
    condition(isString, function(items, item) {
        return items.indexOf(item) > -1;
    });
    condition(isArray, function(items, item) {
        var size = items.length;
        for (var i = 0; i < size; i++) {
            if (items[i] == item) {
                return true;
            }
        }
        return false;
    });
    condition(any, operationNotSupported);
}));
var size = operator(function(items) {
    return items.length;
});
var isEmpty = operator(function(items) {
    return items.length == 0;
});
var notEmpty = function(items) {
    return !isEmpty(items);
};
var collect = operator($witch(function(condition) {
    condition(isString, function(items, collector) {
        return inject(items, '', function(tally, item) {
            return concatenate(tally, collector(item));
        });
    });
    condition(isArray, function(items, collector) {
        return inject(items, [], function(tally, item) {
            return append(tally, collector(item));
        });
    });
    condition(isIndexed, function(items, collector) {
        return Stream(function(cellConstructor) {
            function collectingStream(start, end) {
                if (start > end) return null;
                return function() {
                    return cellConstructor(collector(items[start], start), collectingStream(start + 1, end));
                };
            }
            return collectingStream(0, items.length - 1);
        });
    });
}));
var sort = operator(function(items, sorter) {
    return copy(items).sort(function(a, b) {
        return sorter(a, b) ? -1 : 1;
    });
});
var reverse = operator(function(items) {
    return copy(items).reverse();
});
var copy = operator(function(items) {
    return inject(items, [], curry(append));
});
var join = operator(function(items, separator) {
    return items.join(separator);
});
var inspect = operator();
var reject = function(items, rejector) {
    return select(items, function(i) {
        return !rejector(i);
    });
};
var intersect = operator(function(items, other) {
    return select(items, curry(contains, other));
});
var complement = operator(function(items, other) {
    return reject(items, curry(contains, other));
});
var broadcast = function(items, args) {
    args = args || [];
    each(items, function(i) {
        apply(i, args);
    });
};
var broadcaster = function(items) {
    return function() {
        var args = arguments;
        each(items, function(i) {
            apply(i, args);
        });
    };
};
var asArray = function(items) {
    return inject(items, [], append);
};
var asSet = function(items) {
    return inject(items, [], function(set, item) {
        if (not(contains(set, item))) {
            append(set, item);
        }
        return set;
    });
};
var key = operator();
var value = operator();
function Cell(k, v) {
    return object(function(method) {
        method(key, function(self) {
            return k;
        });
        method(value, function(self) {
            return v;
        });
        method(asString, function(self) {
            return 'Cell[' + asString(k) + ': ' + asString(v) + ']';
        });
    });
}
function Stream(streamDefinition) {
    var stream = streamDefinition(Cell);
    return object(function(method) {
        method(each, function(self, iterator) {
            var cursor = stream;
            while (cursor != null) {
                var cell = cursor();
                iterator(key(cell));
                cursor = value(cell);
            }
        });
        method(inject, function(self, initialValue, injector) {
            var tally = initialValue;
            var cursor = stream;
            while (cursor != null) {
                var cell = cursor();
                tally = injector(tally, key(cell));
                cursor = value(cell);
            }
            return tally;
        });
        method(join, function(self, separator) {
            var tally;
            var cursor = stream;
            while (cursor != null) {
                var cell = cursor();
                var itemAsString = asString(key(cell));
                tally = tally ? tally + separator + itemAsString : itemAsString;
                cursor = value(cell);
            }
            return tally;
        });
        method(collect, function(self, collector) {
            return Stream(function(cellConstructor) {
                function collectingStream(stream) {
                    if (!stream) return null;
                    var cell = stream();
                    return function() {
                        return cellConstructor(collector(key(cell)), collectingStream(value(cell)));
                    };
                }
                return collectingStream(stream);
            });
        });
        method(contains, function(self, item) {
            var cursor = stream;
            while (cursor != null) {
                var cell = cursor();
                if (item == key(cell)) return true;
                cursor = value(cell);
            }
            return false;
        });
        method(size, function(self) {
            var cursor = stream;
            var i = 0;
            while (cursor != null) {
                i++;
                cursor = value(cursor());
            }
            return i;
        });
        method(select, function(self, selector) {
            return Stream(function(cellConstructor) {
                function select(stream) {
                    if (!stream) return null;
                    var cell = stream();
                    var k = key(cell);
                    var v = value(cell);
                    return selector(k) ? function() {
                        return cellConstructor(k, select(v));
                    } : select(v);
                }
                return select(stream);
            });
        });
        method(detect, function(self, detector, notDetectedThunk) {
            var cursor = stream;
            var result;
            while (cursor != null) {
                var cell = cursor();
                var k = key(cell);
                if (detector(k)) {
                    result = k;
                    break;
                }
                cursor = value(cell);
            }
            if (result) {
                return result;
            } else {
                return notDetectedThunk ? notDetectedThunk(self) : null;
            }
        });
        method(isEmpty, function(self) {
            return stream == null;
        });
        method(copy, function(self) {
            return Stream(streamDefinition);
        });
        method(asString, function(self) {
            return 'Stream[' + join(self, ', ') + ']';
        });
    });
}
var indexOf = function(s, substring) {
    var index = s.indexOf(substring);
    if (index >= 0) {
        return index;
    } else {
        throw '"' + s + '" does not contain "' + substring + '"';
    }
};
var lastIndexOf = function(s, substring) {
    var index = s.lastIndexOf(substring);
    if (index >= 0) {
        return index;
    } else {
        throw 'string "' + s + '" does not contain "' + substring + '"';
    }
};
var startsWith = function(s, pattern) {
    return s.indexOf(pattern) == 0;
};
var endsWith = function(s, pattern) {
    return s.lastIndexOf(pattern) == s.length - pattern.length;
};
var containsSubstring = function(s, substring) {
    return s.indexOf(substring) >= 0;
};
var blank = function(s) {
    return /^\s*$/.test(s);
};
var split = function(s, separator) {
    return s.length == 0 ? [] : s.split(separator);
};
var replace = function(s, regex, replace) {
    return s.replace(regex, replace);
};
var toLowerCase = function(s) {
    return s.toLowerCase();
};
var toUpperCase = function(s) {
    return s.toUpperCase();
};
var substring = function(s, from, to) {
    return s.substring(from, to);
};
var asNumber = Number;
var asBoolean = function(s) {
    return 'true' == s || 'any' == s;
};
var asRegexp = function(s) {
    return new RegExp(s);
};
function registerListener(eventType, obj, listener) {
    var previousListener = obj[eventType];
    if (previousListener) {
        obj[eventType] = function() {
            apply(previousListener, arguments);
            apply(listener, arguments);
        };
    } else {
        obj[eventType] = listener;
    }
}
var onLoad = curry(registerListener, 'onload');
var onUnload = curry(registerListener, 'onunload');
var onBeforeUnload = curry(registerListener, 'onbeforeunload');
var onResize = curry(registerListener, 'onresize');
var onKeyPress = curry(registerListener, 'onkeypress');
var onKeyUp = curry(registerListener, 'onkeyup');
window.width = function() {
    return window.innerWidth ? window.innerWidth : (document.documentElement && document.documentElement.clientWidth) ? document.documentElement.clientWidth : document.body.clientWidth;
};
window.height = function() {
    return window.innerHeight ? window.innerHeight : (document.documentElement && document.documentElement.clientHeight) ? document.documentElement.clientHeight : document.body.clientHeight;
};
var debug = operator();
var info = operator();
var warn = operator();
var error = operator();
var childLogger = operator();
var log = operator();
var threshold = operator();
var enable = operator();
var disable = operator();
var toggle = operator();
function Logger(category, handler) {
    return object(function(method) {
        each([debug, info, warn, error], function(priorityOperator) {
            method(priorityOperator, function(self, message, exception) {
                log(handler, priorityOperator, category, message, exception);
            });
        });
        method(childLogger, function(self, categoryName, newHandler) {
            return Logger(append(copy(category), categoryName), newHandler || handler);
        });
        method(asString, function(self) {
            return 'Logger[' + join(category, '.') + ']';
        });
    });
}
function FirebugLogHandler(priority) {
    function formatOutput(category, message) {
        return join(['[', join(category, '.'), '] ', message], '');
    }
    function debugPrimitive(self, category, message, exception) {
        exception ? console.debug(formatOutput(category, message), exception) : console.debug(formatOutput(category, message));
    }
    function infoPrimitive(self, category, message, exception) {
        exception ? console.info(formatOutput(category, message), exception) : console.info(formatOutput(category, message));
    }
    function warnPrimitive(self, category, message, exception) {
        exception ? console.warn(formatOutput(category, message), exception) : console.warn(formatOutput(category, message));
    }
    function errorPrimitive(self, category, message, exception) {
        exception ? console.error(formatOutput(category, message), exception) : console.error(formatOutput(category, message));
    }
    var handlers = [
        Cell(debug, object(function(method) {
            method(debug, debugPrimitive);
            method(info, infoPrimitive);
            method(warn, warnPrimitive);
            method(error, errorPrimitive);
        })),
        Cell(info, object(function(method) {
            method(debug, noop);
            method(info, infoPrimitive);
            method(warn, warnPrimitive);
            method(error, errorPrimitive);
        })),
        Cell(warn, object(function(method) {
            method(debug, noop);
            method(info, noop);
            method(warn, warnPrimitive);
            method(error, errorPrimitive);
        })),
        Cell(error, object(function(method) {
            method(debug, noop);
            method(info, noop);
            method(warn, noop);
            method(error, errorPrimitive);
        }))
    ];
    var handler;
    function selectHandler(p) {
        handler = value(detect(handlers, function(cell) {
            return key(cell) == p;
        }));
    }
    selectHandler(priority || debug);
    return object(function (method) {
        method(threshold, function(self, priority) {
            selectHandler(priority);
        });
        method(log, function(self, operation, category, message, exception) {
            operation(handler, category, message, exception);
        });
    });
}
function WindowLogHandler(thresholdPriority, name) {
    var lineOptions = [25, 50, 100, 200, 400];
    var numberOfLines = lineOptions[3];
    var categoryMatcher = /.*/;
    var closeOnExit = true;
    var logContainer;
    var logEntry = noop;
    function trimLines() {
        var nodes = logContainer.childNodes;
        var trim = size(nodes) - numberOfLines;
        if (trim > 0) {
            each(copy(nodes), function(node, index) {
                if (index < trim) logContainer.removeChild(node);
            });
        }
    }
    function trimAllLines() {
        each(copy(logContainer.childNodes), function(node) {
            logContainer.removeChild(node);
        });
    }
    function toggle() {
        var disabled = logEntry == noop;
        logEntry = disabled ? displayEntry : noop;
        return !disabled;
    }
    function displayEntry(priorityName, colorName, category, message, exception) {
        var categoryName = join(category, '.');
        if (categoryMatcher.test(categoryName)) {
            var elementDocument = logContainer.ownerDocument;
            var timestamp = new Date();
            var completeMessage = join(['[', categoryName, '] : ', message, (exception ? join(['\n', exception.name, ' <', exception.message, '>'], '') : '')], '');
            each(split(completeMessage, '\n'), function(line) {
                if (/(\w+)/.test(line)) {
                    var eventNode = elementDocument.createElement('div');
                    eventNode.style.padding = '3px';
                    eventNode.style.color = colorName;
                    eventNode.setAttribute("title", timestamp + ' | ' + priorityName)
                    logContainer.appendChild(eventNode).appendChild(elementDocument.createTextNode(line));
                }
            });
            logContainer.scrollTop = logContainer.scrollHeight;
        }
        trimLines();
    }
    function showWindow() {
        var logWindow = window.open('', '_blank', 'scrollbars=1,width=800,height=680');
        try {
            var windowDocument = logWindow.document;
            var documentBody = windowDocument.body;
            each(copy(documentBody.childNodes), function(e) {
                windowDocument.body.removeChild(e);
            });
            documentBody.appendChild(windowDocument.createTextNode(' Close on exit '));
            var closeOnExitCheckbox = windowDocument.createElement('input');
            closeOnExitCheckbox.style.margin = '2px';
            closeOnExitCheckbox.setAttribute('type', 'checkbox');
            closeOnExitCheckbox.defaultChecked = true;
            closeOnExitCheckbox.checked = true;
            closeOnExitCheckbox.onclick = function() {
                closeOnExit = closeOnExitCheckbox.checked;
            };
            documentBody.appendChild(closeOnExitCheckbox);
            documentBody.appendChild(windowDocument.createTextNode(' Lines '));
            var lineCountDropDown = windowDocument.createElement('select');
            lineCountDropDown.style.margin = '2px';
            each(lineOptions, function(count, index) {
                var option = lineCountDropDown.appendChild(windowDocument.createElement('option'));
                if (numberOfLines == count) lineCountDropDown.selectedIndex = index;
                option.appendChild(windowDocument.createTextNode(asString(count)));
            });
            documentBody.appendChild(lineCountDropDown);
            documentBody.appendChild(windowDocument.createTextNode(' Category '));
            var categoryInputText = windowDocument.createElement('input');
            categoryInputText.style.margin = '2px';
            categoryInputText.setAttribute('type', 'text');
            categoryInputText.setAttribute('value', categoryMatcher.source);
            categoryInputText.onchange = function() {
                categoryMatcher = new RegExp(categoryInputText.value);
            };
            documentBody.appendChild(categoryInputText);
            documentBody.appendChild(windowDocument.createTextNode(' Level '));
            var levelDropDown = windowDocument.createElement('select');
            levelDropDown.style.margin = '2px';
            var levels = [Cell('debug', debug), Cell('info', info), Cell('warn', warn), Cell('error', error)];
            each(levels, function(priority, index) {
                var option = levelDropDown.appendChild(windowDocument.createElement('option'));
                if (thresholdPriority == value(priority)) levelDropDown.selectedIndex = index;
                option.appendChild(windowDocument.createTextNode(key(priority)));
            });
            levelDropDown.onchange = function(event) {
                thresholdPriority = value(levels[levelDropDown.selectedIndex]);
            };
            documentBody.appendChild(levelDropDown);
            var startStopButton = windowDocument.createElement('input');
            startStopButton.style.margin = '2px';
            startStopButton.setAttribute('type', 'button');
            startStopButton.setAttribute('value', 'Stop');
            startStopButton.onclick = function() {
                startStopButton.setAttribute('value', toggle() ? 'Stop' : 'Start');
            };
            documentBody.appendChild(startStopButton);
            var clearButton = windowDocument.createElement('input');
            clearButton.style.margin = '2px';
            clearButton.setAttribute('type', 'button');
            clearButton.setAttribute('value', 'Clear');
            documentBody.appendChild(clearButton);
            logContainer = documentBody.appendChild(windowDocument.createElement('pre'));
            logContainer.id = 'log-window';
            var logContainerStyle = logContainer.style;
            logContainerStyle.width = '100%';
            logContainerStyle.minHeight = '0';
            logContainerStyle.maxHeight = '550px';
            logContainerStyle.borderWidth = '1px';
            logContainerStyle.borderStyle = 'solid';
            logContainerStyle.borderColor = '#999';
            logContainerStyle.backgroundColor = '#ddd';
            logContainerStyle.overflow = 'scroll';
            lineCountDropDown.onchange = function(event) {
                numberOfLines = lineOptions[lineCountDropDown.selectedIndex];
                trimLines();
            };
            clearButton.onclick = trimAllLines;
            onUnload(window, function() {
                if (closeOnExit) {
                    logEntry = noop;
                    logWindow.close();
                }
            });
        } catch (e) {
            logWindow.close();
        }
    }
    onKeyPress(document, function(evt) {
        var event = $event(evt, document.documentElement);
        var key = keyCode(event);
        if ((key == 20 || key == 84) && isCtrlPressed(event) && isShiftPressed(event)) {
            showWindow();
            logEntry = displayEntry;
        }
    });
    return object(function(method) {
        method(threshold, function(self, priority) {
            thresholdPriority = priority;
        });
        method(log, function(self, operation, category, message, exception) {
            operation(self, category, message, exception);
        });
        method(debug, function(self, category, message, exception) {
            logEntry('debug', '#333', category, message, exception);
        });
        method(info, function(self, category, message, exception) {
            logEntry('info', 'green', category, message, exception);
        });
        method(warn, function(self, category, message, exception) {
            logEntry('warn', 'orange', category, message, exception);
        });
        method(error, function(self, category, message, exception) {
            logEntry('error', 'red', category, message, exception);
        });
    });
}
function lookupCookieValue(name) {
    var tupleString = detect(split(asString(document.cookie), '; '), function(tuple) {
        return startsWith(tuple, name);
    }, function() {
        throw 'Cannot find value for cookie: ' + name;
    });
    return decodeURIComponent(contains(tupleString, '=') ? split(tupleString, '=')[1] : '');
}
function lookupCookie(name, failThunk) {
    try {
        return Cookie(name, lookupCookieValue(name));
    } catch (e) {
        if (failThunk) {
            return failThunk();
        } else {
            throw e;
        }
    }
}
function existsCookie(name) {
    var exists = true;
    lookupCookie(name, function() {
        exists = false;
    });
    return exists;
}
var update = operator();
var remove = operator();
function Cookie(name, val, path) {
    val = val || '';
    path = path || '/';
    document.cookie = name + '=' + val + '; path=' + path;
    return object(function(method) {
        method(value, function(self) {
            return lookupCookieValue(name);
        });
        method(update, function(self, val) {
            document.cookie = name + '=' + encodeURIComponent(val) + '; path=' + path;
            return self;
        });
        method(remove, function(self) {
            var date = new Date();
            date.setTime(date.getTime() - 24 * 60 * 60 * 1000);
            document.cookie = name + '=; expires=' + date.toGMTString() + '; path=' + path;
        });
        method(asString, function(self) {
            return 'Cookie[' + name + ', ' + value(self) + ', ' + path + ']';
        });
    });
}
var run = operator();
var runOnce = operator();
var stop = operator();
function Delay(f, milliseconds) {
    return object(function(method) {
        var id = null;
        method(run, function(self, times) {
            if (id) return;
            var call = times ? function() {
                try {
                    f();
                } finally {
                    if (--times < 1) stop(self);
                }
            } : f;
            id = setInterval(call, milliseconds);
            return self;
        });
        method(runOnce, function(self) {
            return run(self, 1);
        });
        method(stop, function(self) {
            if (!id) return;
            clearInterval(id);
            id = null;
        });
    });
}
function identifier(element) {
    return element.id;
}
function tag(element) {
    return toLowerCase(element.tagName);
}
function property(element, name) {
    return element[name];
}
function parents(element) {
    return Stream(function(cellConstructor) {
        function parentStream(e) {
            if (e == null || e == document) return null;
            return function() {
                return cellConstructor(e, parentStream(e.parentNode));
            };
        }
        return parentStream(element.parentNode);
    });
}
function enclosingForm(element) {
    return element.form || detect(parents(element), function(e) {
        return tag(e) == 'form';
    }, function() {
        throw 'cannot find enclosing form';
    });
}
function enclosingBridge(element) {
    return property(detect(parents(element), function(e) {
        return property(e, 'bridge') != null;
    }, function() {
        throw 'cannot find enclosing bridge';
    }), 'bridge');
}
function serializeElementOn(element, query) {
    var tagName = tag(element);
    switch (tagName) {
        case 'a':
            var name = element.name || element.id;
            if (name) addNameValue(query, name, name);
            break;
        case 'input':
            switch (element.type) {
                case 'image':
                case 'submit':
                case 'button': addNameValue(query, element.name, element.value); break;
            }
            break;
        case 'button':
            if (element.type == 'submit') addNameValue(query, element.name, element.value);
            break;
        default:
    }
}
function $elementWithID(id) {
    return document.getElementById(id);
}
var cancel = operator();
var cancelBubbling = operator();
var cancelDefaultAction = operator();
var isKeyEvent = operator();
var isMouseEvent = operator();
var capturedBy = operator();
var triggeredBy = operator();
var serializeEventOn = operator();
var serializePositionOn = operator();
var type = operator();
var yes = any;
var no = none;
function Event(event, capturingElement) {
    return object(function(method) {
        method(cancel, function(self) {
            cancelBubbling(self);
            cancelDefaultAction(self);
        });
        method(isKeyEvent, no);
        method(isMouseEvent, no);
        method(type, function(self) {
            return event.type;
        });
        method(triggeredBy, function(self) {
            return capturingElement;
        });
        method(capturedBy, function(self) {
            return capturingElement;
        });
        method(serializeEventOn, function(self, query) {
            serializeElementOn(capturingElement, query);
            addNameValue(query, 'ice.event.target', identifier(triggeredBy(self)));
            addNameValue(query, 'ice.event.captured', identifier(capturedBy(self)));
            addNameValue(query, 'ice.event.type', 'on' + type(self));
        });
        method(serializeOn, curry(serializeEventOn));
    });
}
function IEEvent(event, capturingElement) {
    return objectWithAncestors(function(method) {
        method(triggeredBy, function(self) {
            return event.srcElement ? event.srcElement : null;
        });
        method(cancelBubbling, function(self) {
            event.cancelBubble = true;
        });
        method(cancelDefaultAction, function(self) {
            event.returnValue = false;
        });
        method(asString, function(self) {
            return 'IEEvent[' + type(self) + ']';
        });
    }, Event(event, capturingElement));
}
function NetscapeEvent(event, capturingElement) {
    return objectWithAncestors(function(method) {
        method(triggeredBy, function(self) {
            return event.target ? event.target : null;
        });
        method(cancelBubbling, function(self) {
            event.stopPropagation();
        });
        method(cancelDefaultAction, function(self) {
            event.preventDefault();
        });
        method(asString, function(self) {
            return 'NetscapeEvent[' + type(self) + ']';
        });
    }, Event(event, capturingElement));
}
var isAltPressed = operator();
var isCtrlPressed = operator();
var isShiftPressed = operator();
var isMetaPressed = operator();
var serializeKeyOrMouseEventOn = operator();
function KeyOrMouseEvent(event) {
    return object(function(method) {
        method(isAltPressed, function(self) {
            return event.altKey;
        });
        method(isCtrlPressed, function(self) {
            return event.ctrlKey;
        });
        method(isShiftPressed, function(self) {
            return event.shiftKey;
        });
        method(isMetaPressed, function(self) {
            return event.metaKey;
        });
        method(serializeKeyOrMouseEventOn, function(self, query) {
            addNameValue(query, 'ice.event.alt', isAltPressed(self));
            addNameValue(query, 'ice.event.ctrl', isCtrlPressed(self));
            addNameValue(query, 'ice.event.shift', isShiftPressed(self));
            addNameValue(query, 'ice.event.meta', isMetaPressed(self));
        });
    });
}
var isLeftButton = operator();
var isRightButton = operator();
var positionX = operator();
var positionY = operator();
var serializeMouseEventOn = operator();
function MouseEvent(event) {
    return objectWithAncestors(function(method) {
        method(isMouseEvent, yes);
        method(serializeMouseEventOn, function(self, query) {
            serializeKeyOrMouseEventOn(self, query);
            addNameValue(query, 'ice.event.x', positionX(self));
            addNameValue(query, 'ice.event.y', positionY(self));
            addNameValue(query, 'ice.event.left', isLeftButton(self));
            addNameValue(query, 'ice.event.right', isRightButton(self));
        });
    }, KeyOrMouseEvent(event));
}
function MouseEventTrait(method) {
    method(serializeOn, function(self, query) {
        serializeEventOn(self, query);
        serializeMouseEventOn(self, query);
    });
}
function IEMouseEvent(event, capturingElement) {
    return objectWithAncestors(function(method) {
        MouseEventTrait(method);
        method(positionX, function(self) {
            return event.clientX + (document.documentElement.scrollLeft || document.body.scrollLeft);
        });
        method(positionY, function(self) {
            return event.clientY + (document.documentElement.scrollTop || document.body.scrollTop);
        });
        method(isLeftButton, function(self) {
            return event.button == 1;
        });
        method(isRightButton, function(self) {
            return event.button == 2;
        });
        method(asString, function(self) {
            return 'IEMouseEvent[' + type(self) + ']';
        });
    }, IEEvent(event, capturingElement), MouseEvent(event));
}
function NetscapeMouseEvent(event, capturingElement) {
    return objectWithAncestors(function(method) {
        MouseEventTrait(method);
        method(positionX, function(self) {
            return event.pageX;
        });
        method(positionY, function(self) {
            return event.pageY;
        });
        method(isLeftButton, function(self) {
            return event.which == 1;
        });
        method(isRightButton, function(self) {
            return event.which == 2;
        });
        method(asString, function(self) {
            return 'NetscapeMouseEvent[' + type(self) + ']';
        });
    }, NetscapeEvent(event, capturingElement), MouseEvent(event));
}
var keyCharacter = operator();
var keyCode = operator();
var serializeKeyEventOn = operator();
function KeyEvent(event) {
    return objectWithAncestors(function(method) {
        method(isKeyEvent, yes);
        method(keyCharacter, function(self) {
            return String.fromCharCode(keyCode(self));
        });
        method(serializeKeyEventOn, function(self, query) {
            serializeKeyOrMouseEventOn(self, query);
            addNameValue(query, 'ice.event.keycode', keyCode(self));
        });
    }, KeyOrMouseEvent(event));
}
function KeyEventTrait(method) {
    method(serializeOn, function(self, query) {
        serializeEventOn(self, query);
        serializeKeyEventOn(self, query);
    });
}
function IEKeyEvent(event, capturingElement) {
    return objectWithAncestors(function(method) {
        KeyEventTrait(method);
        method(keyCode, function(self) {
            return event.keyCode;
        });
        method(asString, function(self) {
            return 'IEKeyEvent[' + type(self) + ']';
        });
    }, IEEvent(event, capturingElement), KeyEvent(event));
}
function NetscapeKeyEvent(event, capturingElement) {
    return objectWithAncestors(function(method) {
        KeyEventTrait(method);
        method(keyCode, function(self) {
            return event.which == 0 ? event.keyCode : event.which;
        });
        method(asString, function(self) {
            return 'NetscapeKeyEvent[' + type(self) + ']';
        });
    }, NetscapeEvent(event, capturingElement), KeyEvent(event));
}
function isEnterKey(event) {
    return isKeyEvent(event) && keyCode(event) == 13;
}
function isEscKey(event) {
    return isKeyEvent(event) && keyCode(event) == 27;
}
function UnknownEvent(capturingElement) {
    return objectWithAncestors(function(method) {
        method(cancelBubbling, noop);
        method(cancelDefaultAction, noop);
        method(type, function(self) {
            return 'unknown';
        });
        method(asString, function(self) {
            return 'UnkownEvent[]';
        });
    }, Event(null, capturingElement));
}
var MouseListenerNames = [ 'onclick', 'ondblclick', 'onmousedown', 'onmousemove', 'onmouseout', 'onmouseover', 'onmouseup' ];
var KeyListenerNames = [ 'onkeydown', 'onkeypress', 'onkeyup', 'onhelp' ];
function $event(e, element) {
    var capturedEvent = window.event || e;
    if (capturedEvent && capturedEvent.type) {
        var eventType = 'on' + capturedEvent.type;
        if (contains(KeyListenerNames, eventType)) {
            return window.event ? IEKeyEvent(event, element) : NetscapeKeyEvent(e, element);
        } else if (contains(MouseListenerNames, eventType)) {
            return window.event ? IEMouseEvent(event, element) : NetscapeMouseEvent(e, element);
        } else {
            return window.event ? IEEvent(event, element) : NetscapeEvent(e, element);
        }
    } else {
        return UnknownEvent(element);
    }
}
var asURIEncodedString = operator();
var serializeOn = operator();
var Parameter = function(name, value) {
    return objectWithAncestors(function(method) {
        method(asURIEncodedString, function(self) {
            return encodeURIComponent(name) + '=' + encodeURIComponent(value);
        });
        method(serializeOn, function(self, query) {
            addParameter(query, self);
        });
    }, Cell(name, value));
};
var addParameter = operator();
var addNameValue = operator();
var queryParameters = operator();
var addQuery = operator();
var appendToURI = operator();
var Query = function() {
    var parameters = [];
    return object(function(method) {
        method(queryParameters, function(self) {
            return parameters;
        });
        method(addParameter, function(self, parameter) {
            append(parameters, parameter);
            return self;
        });
        method(addNameValue, function(self, name, value) {
            append(parameters, Parameter(name, value));
            return self;
        });
        method(addQuery, function(self, appended) {
            serializeOn(appended, self);
            return self;
        });
        method(serializeOn, function(self, query) {
            each(parameters, curry(addParameter, query));
        });
        method(asURIEncodedString, function(self) {
            return join(collect(parameters, asURIEncodedString), '&');
        });
        method(appendToURI, function(self, uri) {
            if (not(isEmpty(parameters))) {
                return uri + (contains(uri, '?') ? '&' : '?') + asURIEncodedString(self);
            } else {
                return uri;
            }
        });
        method(asString, function(self) {
            return inject(parameters, '', function(tally, p) {
                return tally + '|' + key(p) + '=' + value(p) + '|\n';
            });
        });
    });
};
var getSynchronously = operator();
var getAsynchronously = operator();
var postSynchronously = operator();
var postAsynchronously = operator();
var Client = function(autoclose) {
    var newNativeRequest;
    if (window.XMLHttpRequest) {
        newNativeRequest = function() {
            return new XMLHttpRequest();
        };
    } else if (window.ActiveXObject) {
        newNativeRequest = function() {
            return new window.ActiveXObject('Microsoft.XMLHTTP');
        };
    } else {
        throw 'cannot create XMLHttpRequest';
    }
    function withNewQuery(setup) {
        var query = Query();
        setup(query);
        return query;
    }
    var autoClose = autoclose ? close : noop;
    return object(function(method) {
        method(getAsynchronously, function(self, uri, setupQuery, setupRequest, onResponse) {
            var nativeRequestResponse = newNativeRequest();
            var request = RequestProxy(nativeRequestResponse);
            var response = ResponseProxy(nativeRequestResponse);
            nativeRequestResponse.open('GET', appendToURI(withNewQuery(setupQuery), uri), true);
            setupRequest(request);
            nativeRequestResponse.onreadystatechange = function() {
                if (nativeRequestResponse.readyState == 4) {
                    onResponse(response, request);
                    autoClose(request);
                }
            };
            nativeRequestResponse.send('');
            return request;
        });
        method(getSynchronously, function(self, uri, setupQuery, setupRequest, onResponse) {
            var nativeRequestResponse = newNativeRequest();
            var request = RequestProxy(nativeRequestResponse);
            var response = ResponseProxy(nativeRequestResponse);
            nativeRequestResponse.open('GET', appendToURI(withNewQuery(setupQuery), uri), false);
            setupRequest(request);
            nativeRequestResponse.send('');
            onResponse(response, request);
            autoClose(request);
        });
        method(postAsynchronously, function(self, uri, setupQuery, setupRequest, onResponse) {
            var nativeRequestResponse = newNativeRequest();
            var request = RequestProxy(nativeRequestResponse);
            var response = ResponseProxy(nativeRequestResponse);
            nativeRequestResponse.open('POST', uri, true);
            setupRequest(request);
            nativeRequestResponse.onreadystatechange = function() {
                if (nativeRequestResponse.readyState == 4) {
                    onResponse(response, request);
                    autoClose(request);
                }
            };
            nativeRequestResponse.send(asURIEncodedString(withNewQuery(setupQuery)));
            return request;
        });
        method(postSynchronously, function(self, uri, setupQuery, setupRequest, onResponse) {
            var nativeRequestResponse = newNativeRequest();
            var request = RequestProxy(nativeRequestResponse);
            var response = ResponseProxy(nativeRequestResponse);
            nativeRequestResponse.open('POST', uri, false);
            setupRequest(request);
            nativeRequestResponse.send(asURIEncodedString(withNewQuery(setupQuery)));
            onResponse(response, request);
            autoClose(request);
        });
    });
};
var close = operator();
var abort = operator();
var setHeader = operator();
var onResponse = operator();
var RequestProxy = function(nativeRequestResponse) {
    return object(function(method) {
        method(setHeader, function(self, name, value) {
            nativeRequestResponse.setRequestHeader(name, value);
        });
        method(close, function(self) {
            nativeRequestResponse.onreadystatechange = noop;
        });
        method(abort, function(self) {
            nativeRequestResponse.onreadystatechange = noop;
            nativeRequestResponse.abort();
            method(abort, noop);
        });
    });
};
var statusCode = operator();
var statusText = operator();
var getHeader = operator();
var getAllHeaders = operator();
var hasHeader = operator();
var contentAsText = operator();
var contentAsDOM = operator();
var ResponseProxy = function(nativeRequestResponse) {
    return object(function(method) {
        method(statusCode, function() {
            try {
                return nativeRequestResponse.status;
            } catch (e) {
                return 0;
            }
        });
        method(statusText, function(self) {
            try {
                return nativeRequestResponse.statusText;
            } catch (e) {
                return '';
            }
        });
        method(hasHeader, function(self, name) {
            try {
                var header = nativeRequestResponse.getResponseHeader(name);
                return header && header != '';
            } catch (e) {
                return false;
            }
        });
        method(getHeader, function(self, name) {
            try {
                return nativeRequestResponse.getResponseHeader(name);
            } catch (e) {
                return null;
            }
        });
        method(getAllHeaders, function(self, name) {
            try {
                return collect(reject(split(nativeRequestResponse.getAllResponseHeaders(), '\n'), isEmpty), function(pair) {
                    var nameValue = split(pair, ': ')
                    return Cell(nameValue[0], nameValue[1]);
                });
            } catch (e) {
                return [];
            }
        });
        method(contentAsText, function(self) {
            try {
                return nativeRequestResponse.responseText;
            } catch (e) {
                return '';
            }
        });
        method(contentAsDOM, function(self) {
            return nativeRequestResponse.responseXML;
        });
        method(asString, function(self) {
            return inject(getAllHeaders(self), 'HTTP Response\n', function(result, header) {
                return result + key(header) + ': ' + value(header) + '\n';
            }) + contentAsText(self);
        });
    });
};
function OK(response) {
    return statusCode(response) == 200;
}
function NotFound(response) {
    return statusCode(response) == 404;
}
function ServerInternalError(response) {
    var code = statusCode(response);
    return code >= 500 && code < 600;
}
function FormPost(request) {
    setHeader(request, 'Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
}
var register = operator();
var deserializeAndExecute = operator();
function CommandDispatcher() {
    var commands = [];
    return object(function(method) {
        method(register, function(self, messageName, command) {
            commands = reject(commands, function(cell) {
                return key(cell) == messageName;
            });
            append(commands, Cell(messageName, command));
        });
        method(deserializeAndExecute, function(self, message) {
            var messageName = message.nodeName;
            var found = detect(commands, function(cell) {
                return key(cell) == messageName;
            }, function() {
                throw 'Unknown message received: ' + messageName;
            });
            value(found)(message);
        });
    });
}
function ParsingError(message) {
    logger.error('Parsing error');
    var errorNode = message.firstChild;
    logger.error(errorNode.data);
    var sourceNode = errorNode.firstChild;
    logger.error(sourceNode.data);
}
var send = operator();
var onSend = operator();
var onReceive = operator();
var onServerError = operator();
var whenDown = operator();
var whenTrouble = operator();
var shutdown = operator();
var resetConnection = operator();
function AsyncConnection(logger, windowID, receiveURI) {
    var logger = childLogger(logger, 'async-connection');
    var channel = Client(false);
    var onReceiveListeners = [];
    var onServerErrorListeners = [];
    var connectionDownListeners = [];
    var connectionTroubleListeners = [];
    var listener = object(function(method) {
        method(close, noop);
        method(abort, noop);
    });
    var listening = object(function(method) {
        method(remove, noop);
    });
    onBeforeUnload(window, function() {
        connectionDownListeners = [];
    });
    var serverErrorCallback = broadcaster(onServerErrorListeners);
    var receiveCallback = broadcaster(onReceiveListeners);
    var sendXWindowCookie = noop;
    var receiveXWindowCookie = function (response) {
        var xWindowCookie = getHeader(response, "X-Set-Window-Cookie");
        if (xWindowCookie) {
            sendXWindowCookie = function(request) {
                setHeader(request, "X-Window-Cookie", xWindowCookie);
            };
        }
    };
    try {
        listening = lookupCookie('ice.connection.running');
        remove(listening);
    } catch (e) {
    }
    function timedRetryAbort(retryAction, abortAction, timeouts) {
        var index = 0;
        var errorActions = inject(timeouts, [abortAction], function(actions, interval) {
            return insert(actions, curry(runOnce, Delay(retryAction, interval)));
        });
        return function() {
            if (index < errorActions.length) {
                apply(errorActions[index], arguments);
                index++;
            }
        };
    }
    function registeredPushIds() {
        try {
            return split(lookupCookieValue('ice.pushids'), ' ');
        } catch (e) {
            return [];
        }
    }
    var lastSentPushIds = registeredPushIds();
    function connect() {
        try {
            debug(logger, "closing previous connection...");
            close(listener);
            lastSentPushIds = registeredPushIds();
            if (isEmpty(lastSentPushIds)) {
                offerCandidature();
            } else {
                debug(logger, "connect...");
                listener = postAsynchronously(channel, receiveURI, function(q) {
                    each(lastSentPushIds, curry(addNameValue, q, 'ice.pushid'));
                }, function(request) {
                    FormPost(request);
                    sendXWindowCookie(request);
                    setHeader(request, "ice.push.window", namespace.windowID);
                }, $witch(function (condition) {
                    condition(OK, function(response) {
                        var reconnect = getHeader(response, 'X-Connection') != 'close';
                        var nonEmptyResponse = notEmpty(contentAsText(response));
                        if (reconnect) {
                            if (not(nonEmptyResponse)) warn(logger, 'empty response received');
                        } else {
                            info(logger, 'blocking connection stopped at server\'s request...');
                        }
                        if (nonEmptyResponse) receiveCallback(response);
                        receiveXWindowCookie(response);
                        if (reconnect) connect();
                    });
                    condition(ServerInternalError, retryOnServerError);
                }));
            }
        } catch (e) {
            error(logger, 'failed to re-initiate blocking connection', e);
        }
    }
    var configuration = namespace.push.configuration;
    var retryOnServerError = timedRetryAbort(connect, serverErrorCallback, configuration.serverErrorRetryTimeouts || [1000, 2000, 4000]);
    var heartbeatTimeout = configuration.heartbeat && configuration.heartbeat.timeout ? configuration.heartbeat.timeout : 30000;
    function initializeConnection() {
        connect();
    }
    var pollingPeriod = 1000;
    var contextPath = namespace.push.configuration.contextPath;
    var leaseCookie = lookupCookie('ice.connection.lease', function() {
        return Cookie('ice.connection.lease', asString((new Date).getTime()));
    });
    var connectionCookie = listening = lookupCookie('ice.connection.running', function() {
        return Cookie('ice.connection.running', '');
    });
    var contextPathCookie = lookupCookie('ice.connection.contextpath', function() {
        return Cookie('ice.connection.contextpath', contextPath);
    });
    function updateLease() {
        update(leaseCookie, (new Date).getTime() + pollingPeriod * 2);
    }
    function isLeaseExpired() {
        return asNumber(value(leaseCookie)) < (new Date).getTime();
    }
    function shouldEstablishBlockingConnection() {
        return !existsCookie('ice.connection.running') || isEmpty(lookupCookieValue('ice.connection.running'));
    }
    function offerCandidature() {
        update(connectionCookie, windowID);
    }
    function isWinningCandidate() {
        return startsWith(value(connectionCookie), windowID);
    }
    function markAsOwned() {
        update(connectionCookie, windowID + ':acquired');
        update(contextPathCookie, contextPath);
    }
    function isOwner() {
        return value(connectionCookie) == (windowID + ':acquired');
    }
    function hasOwner() {
        return endsWith(value(connectionCookie), ':acquired');
    }
    function nonMatchingContextPath() {
        return value(contextPathCookie) != contextPath;
    }
    if (nonMatchingContextPath()) {
        offerCandidature();
        info(logger, 'Blocking connection cannot be shared among multiple web-contexts.\nInitiating blocking connection for "' + contextPath + '"  web-context...');
    }
    var blockingConnectionMonitor = run(Delay(function() {
        if (shouldEstablishBlockingConnection()) {
            offerCandidature();
            info(logger, 'blocking connection not initialized...candidate for its creation');
        } else {
            if (isWinningCandidate()) {
                if (!hasOwner()) {
                    markAsOwned();
                    if (notEmpty(registeredPushIds())) {
                        initializeConnection();
                    }
                }
                updateLease();
            }
            if (hasOwner() && isLeaseExpired()) {
                offerCandidature();
                info(logger, 'blocking connection lease expired...candidate for its creation');
            }
        }
        if (isOwner()) {
            var ids = registeredPushIds();
            if ((size(ids) != size(lastSentPushIds)) || notEmpty(complement(ids, lastSentPushIds))) {
                abort(listener);
                connect();
            }
        } else {
            abort(listener);
        }
    }, pollingPeriod));
    info(logger, 'asynchronous mode');
    return object(function(method) {
        method(onReceive, function(self, callback) {
            append(onReceiveListeners, callback);
        });
        method(onServerError, function(self, callback) {
            append(onServerErrorListeners, callback);
        });
        method(whenDown, function(self, callback) {
            append(connectionDownListeners, callback);
        });
        method(whenTrouble, function(self, callback) {
            append(connectionTroubleListeners, callback);
        });
        method(shutdown, function(self) {
            try {
                method(shutdown, noop);
                connect = noop;
            } catch (e) {
                error(logger, 'error during shutdown', e);
            } finally {
                onReceiveListeners = connectionDownListeners = onServerErrorListeners = [];
                abort(listener);
                stop(blockingConnectionMonitor);
                remove(listening);
            }
        });
    });
}
        var notificationListeners = [];
        namespace.onNotification = function(callback) {
            append(notificationListeners, callback);
        };
        var serverErrorListeners = [];
        namespace.onBlockingConnectionServerError = function(callback) {
            append(serverErrorListeners, callback);
        };
        var blockingConnectionUnstableListeners = [];
        namespace.onBlockingConnectionUnstable = function(callback) {
            append(blockingConnectionUnstableListeners, callback);
        };
        var blockingConnectionLostListeners = [];
        namespace.onBlockingConnectionLost = function(callback) {
            append(blockingConnectionLostListeners, callback);
        };
        var handler = window.console && window.console.firebug ? FirebugLogHandler(debug) : WindowLogHandler(debug, window.location.href);
        namespace.windowID = namespace.windowID || substring(Math.random().toString(16), 2, 7);
        namespace.logger = Logger([ 'icepush' ], handler);
        namespace.info = info;
        var pushIdentifiers = [];
        function enlistPushIDsWithBrowser(ids) {
            try {
                var idsCookie = lookupCookie('ice.pushids');
                var registeredIDs = split(value(idsCookie), ' ');
                update(idsCookie, join(concatenate(registeredIDs, ids), ' '));
            } catch (e) {
                Cookie('ice.pushids', join(ids, ' '));
            }
        }
        function delistPushIDsWithBrowser(ids) {
            if (existsCookie('ice.pushids')) {
                var idsCookie = lookupCookie('ice.pushids');
                var registeredIDs = split(value(idsCookie), ' ');
                update(idsCookie, join(complement(registeredIDs, ids), ' '));
            }
        }
        function enlistPushIDsWithWindow(ids) {
            enlistPushIDsWithBrowser(ids);
            pushIdentifiers = concatenate(pushIdentifiers, ids);
        }
        function delistPushIDsWithWindow(ids) {
            delistPushIDsWithBrowser(ids);
            pushIdentifiers = complement(pushIdentifiers, ids);
        }
        onBeforeUnload(window, function() {
            delistPushIDsWithBrowser(pushIdentifiers);
            pushIdentifiers = [];
        });
        function throwServerError(response) {
            throw 'Server internal error: ' + contentAsText(response);
        }
        function calculateURI(uri) {
            return (namespace.push.configuration.uriPrefix || '') + uri + (namespace.push.configuration.uriSuffix || '');
        }
        var currentNotifications = [];
        var apiChannel = Client(true);
        namespace.uriextension = '';
        namespace.push = {
            register: function(pushIds, callback) {
                if ((typeof callback) == 'function') {
                    enlistPushIDsWithWindow(pushIds);
                    namespace.onNotification(function(ids) {
                        currentNotifications = asArray(intersect(ids, pushIds));
                        if (notEmpty(currentNotifications)) {
                            try {
                                callback(currentNotifications);
                            } catch (e) {
                                error(namespace.logger, 'error thrown by push notification callback', e);
                            }
                        }
                    });
                } else {
                    throw 'the callback is not a function';
                }
            },
            deregister: delistPushIDsWithWindow,
            getCurrentNotifications: function() {
                return currentNotifications;
            },
            createPushId: function() {
                var id;
                postSynchronously(apiChannel, calculateURI('create-push-id.icepush'), noop, FormPost, $witch(function (condition) {
                    condition(OK, function(response) {
                        id = contentAsText(response);
                    });
                    condition(ServerInternalError, throwServerError);
                }));
                return id;
            },
            notify: function(id) {
                postAsynchronously(apiChannel, calculateURI('notify.icepush'), function(q) {
                    addNameValue(q, 'id', id);
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },
            addGroupMember: function(group, id) {
                postAsynchronously(apiChannel, calculateURI('add-group-member.icepush'), function(q) {
                    addNameValue(q, 'group', group);
                    addNameValue(q, 'id', id);
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },
            removeGroupMember: function(group, id) {
                postAsynchronously(apiChannel, calculateURI('remove-group-member.icepush'), function(q) {
                    addNameValue(q, 'group', group);
                    addNameValue(q, 'id', id);
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },
            get: function(uri, parameters, responseCallback) {
                getAsynchronously(apiChannel, uri, function(query) {
                    parameters(curry(addNameValue, query));
                }, noop, $witch(function(condition) {
                    condition(OK, function(response) {
                        responseCallback(statusCode(response), contentAsText(response), contentAsDOM(response));
                    });
                    condition(ServerInternalError, throwServerError);
                }));
            },
            post: function(uri, parameters, responseCallback) {
                postAsynchronously(apiChannel, uri, function(query) {
                    parameters(curry(addNameValue, query));
                }, FormPost, $witch(function(condition) {
                    condition(OK, function(response) {
                        responseCallback(statusCode(response), contentAsText(response), contentAsDOM(response));
                    });
                    condition(ServerInternalError, throwServerError);
                }));
            },
            searchAndEvaluateScripts: function(element) {
                each(element.getElementsByTagName('script'), function(script) {
                    var newScript = document.createElement('script');
                    newScript.setAttribute('type', 'text/javascript');
                    if (script.src) {
                        newScript.src = script.src;
                    } else {
                        newScript.text = script.text;
                    }
                    element.appendChild(newScript);
                });
            },
            configuration: {
                uriSuffix: '',
                uriPrefix: ''
            }
        };
        function Bridge(configuration) {
            var windowID = configuration.window;
            var logger = childLogger(namespace.logger, windowID);
            var commandDispatcher = CommandDispatcher();
            var asyncConnection = AsyncConnection(logger, windowID, calculateURI('listen.icepush'));
            register(commandDispatcher, 'noop', function() {
                debug(logger, 'received noop');
            });
            register(commandDispatcher, 'parsererror', ParsingError);
            function purgeUnusedPushIDs(ids) {
                var registeredIDsCookie = lookupCookie('ice.pushids');
                var registeredIDs = split(value(registeredIDsCookie), ' ');
                return intersect(ids, registeredIDs);
            }
            var notifiedPushIDs = lookupCookie('ice.notified.pushids', function() {
                return Cookie('ice.notified.pushids', '');
            });
            register(commandDispatcher, 'notified-pushids', function(message) {
                var text = message.firstChild;
                if (text && !blank(text.data)) {
                    var ids = split(value(notifiedPushIDs), ' ');
                    var receivedPushIDs = split(text.data, ' ');
                    debug(logger, 'received notifications: ' + receivedPushIDs);
                    update(notifiedPushIDs, join(purgeUnusedPushIDs(asSet(concatenate(ids, receivedPushIDs))), ' '));
                } else {
                    warn(logger, "No notification was received.");
                }
            });
            var notificationMonitor = run(Delay(function() {
                try {
                    var ids = split(value(notifiedPushIDs), ' ');
                    if (notEmpty(ids)) {
                        broadcast(notificationListeners, [ ids ]);
                        debug(logger, 'picked up notifications for this window: ' + ids);
                        update(notifiedPushIDs, join(purgeUnusedPushIDs(complement(ids, pushIdentifiers)), ' '));
                    }
                } catch (e) {
                    warn(logger, 'failed to listen for updates', e);
                }
            }, 300));
            function dispose() {
                try {
                    dispose = noop;
                    stop(notificationMonitor);
                } finally {
                    shutdown(asyncConnection);
                }
            }
            onUnload(window, dispose);
            onReceive(asyncConnection, function(response) {
                var mimeType = getHeader(response, 'Content-Type');
                if (mimeType && startsWith(mimeType, 'text/xml')) {
                    deserializeAndExecute(commandDispatcher, contentAsDOM(response).documentElement);
                } else {
                    warn(logger, 'unknown content in response - ' + mimeType + ', expected text/xml');
                }
            });
            onServerError(asyncConnection, function(response) {
                try {
                    warn(logger, 'server side error');
                    broadcast(serverErrorListeners, [ statusCode(response), contentAsText(response), contentAsDOM(response) ]);
                } finally {
                    dispose();
                }
            });
            whenDown(asyncConnection, function(reconnectAttempts) {
                try {
                    warn(logger, 'connection to server was lost');
                    broadcast(blockingConnectionLostListeners, [ reconnectAttempts ]);
                } finally {
                    dispose();
                }
            });
            whenTrouble(asyncConnection, function() {
                warn(logger, 'connection in trouble');
                broadcast(blockingConnectionUnstableListeners);
            });
            info(logger, 'bridge loaded!');
        }
        onLoad(window, function() {
            Bridge({window: namespace.windowID, connection: {}});
        });
        onKeyPress(document, function(ev) {
            var e = $event(ev);
            if (isEscKey(e)) cancelDefaultAction(e);
        });
    })(window.ice);
}
