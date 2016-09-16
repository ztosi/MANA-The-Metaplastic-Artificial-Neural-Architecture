function [ h ] = overlayedHistogram( values, varargin )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    data = [];
    dcell = {};
    if iscell(values)
        numSets = legnth(values);
        for i = 1:length(values)
            if iscolumn(values{i})
                values{i} = values{i}';
            end
            data = [data values{i}];
            dcell(i,1) = values(i);
        end
        [~, ed] = histcounts(data, varargin{:});
        prevHeld = 1;
        if ~ishold
            figure; hold on;
            prevHeld = 0;
        end  
        h=[];
        for i = 1:length(dcell)
           h = [h histogram(data, ed, varargin{:})];
        end
        if ~prevHeld
            hold off;
        end
    else
        numSets = 1;
        data = values;
        if iscolumn(data)
            data=data';
        end
        dcell{1, 1} = values; 
        binSpec = 0;
        for i = 1:length(varargin)
            if isstring(varargin{i}) || isscalar(varargin{i}) ...
                    || ischar(varargin{i})
               binSpec = isscalar(varargin{i});
               numSets = i;
               break;
            end
            if iscolumn(varargin{i})
               data = [data varargin{i}'];
            else
               data = [data varargin{i}];
            end
            dcell(i+1, 1) = varargin(i);
            numSets = numSets + 1;
        end
        [~, ed] = histcounts(data, varargin{numSets:end});
        numSets = numSets + binSpec;
        prevHeld = 1;
        if ~ishold
            figure; hold on;
            prevHeld = 0;
        end
        h= [];
        for i = 1:length(dcell)
           h = [h histogram(dcell{i}, ed, varargin{numSets:end})];
        end
        if ~prevHeld
           hold off;
        end
    end
    
    alph = 1/length(h);
    if alph < 0.1
        alph = 0.1;
    end
    for i=1:length(h)
        h(i).FaceAlpha = alph;
        h(i).EdgeColor = 'none';
    end
end

